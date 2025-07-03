package cloud.providers.gcp

import cloud.*
import io.circe.*
import io.circe.syntax.*

object DeploymentManagerGenerator:
  given Encoder[CloudConfig] = Encoder.instance { config =>
    Json.obj(
      config.map { case (key, value) =>
        key -> encodeValue(value)
      }.toSeq*
    )
  }

  private def encodeValue(value: Any): Json = value match
    case s: String  => s.asJson
    case i: Int     => i.asJson
    case d: Double  => d.asJson
    case b: Boolean => b.asJson
    case l: List[_] =>
      Json.fromValues(l.map(encodeValue))
    case m: Map[_, _] =>
      Json.obj(
        m.asInstanceOf[Map[String, Any]]
          .map((k, v) => k -> encodeValue(v))
          .toSeq*
      )
    case _ => value.toString.asJson

  def generate(app: CloudApp): Json =
    Json.obj(
      "resources" -> Json.fromValues(app.resources.map(generateResource))
    )

  private def generateResource(resource: CloudResource): Json =
    val baseResource = Json.obj(
      "name" -> resource.name.asJson,
      "type" -> mapResourceType(resource).asJson,
      "properties" -> mapProperties(resource).asJson
    )

    if (resource.dependencies.nonEmpty) {
      baseResource.deepMerge(
        Json.obj(
          "metadata" -> Json.obj(
            "dependsOn" -> resource.dependencies.map(_.name).asJson
          )
        )
      )
    } else {
      baseResource
    }

  private def mapResourceType(resource: CloudResource): String = resource match
    case _: ObjectStorage           => "storage.v1.bucket"
    case _: ServerlessFunction      => "cloudfunctions.v1.function"
    case _: NoSqlTable              => "firestore.v1.database"
    case _: VirtualNetwork          => "compute.v1.network"
    case _: SecurityGroup           => "compute.v1.firewall"
    case _: ApplicationLoadBalancer => "compute.v1.forwardingRule"

  private def mapProperties(resource: CloudResource): Json = resource match
    case storage: ObjectStorage       => mapStorageProperties(storage)
    case function: ServerlessFunction => mapFunctionProperties(function)
    case table: NoSqlTable            => mapTableProperties(table)
    case network: VirtualNetwork      => mapNetworkProperties(network)
    case sg: SecurityGroup            => mapSecurityGroupProperties(sg)
    case alb: ApplicationLoadBalancer => mapLoadBalancerProperties(alb)

  private def mapStorageProperties(storage: ObjectStorage): Json =
    val baseProps = Json.obj(
      "location" -> "US".asJson,
      "storageClass" -> "STANDARD".asJson
    )

    // map versioning if present
    storage.properties.get("VersioningConfiguration") match
      case Some(_) =>
        baseProps.deepMerge(
          Json.obj(
            "versioning" -> Json.obj("enabled" -> true.asJson)
          )
        )
      case None => baseProps

  private def mapFunctionProperties(function: ServerlessFunction): Json =
    val runtime = function.properties.getOrElse("Runtime", "nodejs18")
    val handler = function.properties.getOrElse("Handler", "index.handler")

    Json.obj(
      "location" -> "us-central1".asJson,
      "runtime" -> mapGcpRuntime(runtime.toString).asJson,
      "entryPoint" -> handler.toString.split("\\.").head.asJson,
      "sourceArchiveUrl" -> "gs://my-source-bucket/function.zip".asJson
    )

  private def mapTableProperties(table: NoSqlTable): Json =
    Json.obj(
      "location" -> "us-central1".asJson,
      "type" -> "FIRESTORE_NATIVE".asJson
    )

  private def mapGcpRuntime(awsRuntime: String): String = awsRuntime match
    case r if r.startsWith("nodejs") => "nodejs18"
    case r if r.startsWith("python") => "python39"
    case r if r.startsWith("java")   => "java17"
    case _                           => "nodejs18"

  private def mapNetworkProperties(network: VirtualNetwork): Json =
    Json.obj(
      "autoCreateSubnetworks" -> false.asJson,
      "routingConfig" -> Json.obj(
        "routingMode" -> "REGIONAL".asJson
      )
    )

  private def mapSecurityGroupProperties(sg: SecurityGroup): Json =
    val baseProps = Json.obj(
      "direction" -> "INGRESS".asJson,
      "priority" -> 1000.asJson,
      "network" -> "global/networks/default".asJson
    )

    sg.properties.get("SecurityGroupIngress") match
      case Some(rules: List[_]) =>
        val gcpRules = rules.asInstanceOf[List[Map[String, Any]]].map { rule =>
          Json.obj(
            "IPProtocol" -> rule("IpProtocol").toString.toLowerCase.asJson,
            "ports" -> Json.fromValues(Seq(rule("FromPort").toString.asJson))
          )
        }
        baseProps.deepMerge(
          Json.obj(
            "allowed" -> Json.fromValues(gcpRules),
            "sourceRanges" -> Json.fromValues(Seq("0.0.0.0/0".asJson))
          )
        )
      case None => baseProps

  private def mapLoadBalancerProperties(alb: ApplicationLoadBalancer): Json =
    val baseProps = Json.obj(
      "region" -> "us-central1".asJson,
      "loadBalancingScheme" -> "EXTERNAL".asJson,
      "portRange" -> "80".asJson
    )

    alb.properties.get("Targets") match
      case Some(targets: List[_]) =>
        baseProps.deepMerge(
          Json.obj(
            "target" -> "global/targetHttpProxies/my-target-proxy".asJson,
            "backendServices" -> Json.obj(
              "backends" -> Json.fromValues(
                targets
                  .asInstanceOf[List[String]]
                  .map(target =>
                    Json.obj(
                      "group" -> s"zones/us-central1-a/instanceGroups/$target".asJson
                    )
                  )
              )
            )
          )
        )
      case None => baseProps
