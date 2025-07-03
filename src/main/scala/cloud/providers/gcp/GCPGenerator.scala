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
    case _: ObjectStorage      => "storage.v1.bucket"
    case _: ServerlessFunction => "cloudfunctions.v1.function"
    case _: NoSqlTable         => "firestore.v1.database"
    case _: VirtualNetwork     => "compute.v1.network"

  private def mapProperties(resource: CloudResource): Json = resource match
    case storage: ObjectStorage       => mapStorageProperties(storage)
    case function: ServerlessFunction => mapFunctionProperties(function)
    case table: NoSqlTable            => mapTableProperties(table)
    case network: VirtualNetwork      => mapNetworkProperties(network)

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
