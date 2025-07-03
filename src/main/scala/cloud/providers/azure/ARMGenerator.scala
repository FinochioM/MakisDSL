package cloud.providers.azure

import cloud.*
import io.circe.*
import io.circe.syntax.*

object ARMGenerator:
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
      "$schema" -> "https://schema.management.azure.com/schemas/2019-04-01/deploymentTemplate.json#".asJson,
      "contentVersion" -> "1.0.0.0".asJson,
      "resources" -> Json.fromValues(app.resources.map(generateResource))
    )

  private def generateResource(resource: CloudResource): Json =
    val baseResource = Json.obj(
      "type" -> mapResourceType(resource).asJson,
      "apiVersion" -> getApiVersion(resource).asJson,
      "name" -> resource.name.asJson,
      "location" -> "[resourceGroup().location]".asJson,
      "properties" -> mapProperties(resource).asJson
    )

    if (resource.dependencies.nonEmpty) {
      baseResource.deepMerge(
        Json.obj(
          "dependsOn" -> resource.dependencies.map(_.name).asJson
        )
      )
    } else {
      baseResource
    }

  private def mapResourceType(resource: CloudResource): String = resource match
    case _: ObjectStorage           => "Microsoft.Storage/storageAccounts"
    case _: ServerlessFunction      => "Microsoft.Web/sites"
    case _: NoSqlTable              => "Microsoft.DocumentDB/databaseAccounts"
    case _: VirtualNetwork          => "Microsoft.Network/virtualNetworks"
    case _: SecurityGroup           => "Microsoft.Network/networkSecurityGroups"
    case _: ApplicationLoadBalancer => "Microsoft.Network/loadBalancers"

  private def getApiVersion(resource: CloudResource): String = resource match
    case _: ObjectStorage           => "2023-01-01"
    case _: ServerlessFunction      => "2023-01-01"
    case _: NoSqlTable              => "2023-04-15"
    case _: VirtualNetwork          => "2023-04-01"
    case _: SecurityGroup           => "2023-04-01"
    case _: ApplicationLoadBalancer => "2023-04-01"

  private def mapProperties(resource: CloudResource): Json = resource match
    case storage: ObjectStorage       => mapStorageProperties(storage)
    case function: ServerlessFunction => mapFunctionProperties(function)
    case table: NoSqlTable            => mapTableProperties(table)
    case network: VirtualNetwork      => mapNetworkProperties(network)
    case sg: SecurityGroup            => mapSecurityGroupProperties(sg)
    case alb: ApplicationLoadBalancer => mapLoadBalancerProperties(alb)

  private def mapStorageProperties(storage: ObjectStorage): Json =
    val baseProps = Json.obj(
      "sku" -> Json.obj("name" -> "Standard_LRS".asJson),
      "kind" -> "StorageV2".asJson,
      "accessTier" -> "Hot".asJson
    )

    // map versioning if present
    storage.properties.get("VersioningConfiguration") match
      case Some(_) =>
        baseProps.deepMerge(Json.obj("isVersioningEnabled" -> true.asJson))
      case None => baseProps

  private def mapFunctionProperties(function: ServerlessFunction): Json =
    Json.obj(
      "kind" -> "functionapp".asJson,
      "siteConfig" -> Json.obj(
        "appSettings" -> Json.fromValues(
          Seq(
            Json.obj(
              "name" -> "FUNCTIONS_WORKER_RUNTIME".asJson,
              "value" -> "node".asJson
            ),
            Json.obj(
              "name" -> "WEBSITE_NODE_DEFAULT_VERSION".asJson,
              "value" -> "~18".asJson
            )
          )
        )
      )
    )

  private def mapTableProperties(table: NoSqlTable): Json =
    Json.obj(
      "databaseAccountOfferType" -> "Standard".asJson,
      "consistencyPolicy" -> Json.obj(
        "defaultConsistencyLevel" -> "Session".asJson
      ),
      "locations" -> Json.fromValues(
        Seq(
          Json.obj(
            "locationName" -> "[resourceGroup().location]".asJson,
            "failoverPriority" -> 0.asJson
          )
        )
      )
    )

  private def mapNetworkProperties(network: VirtualNetwork): Json =
    Json.obj(
      "addressSpace" -> Json.obj(
        "addressPrefixes" -> Json.fromValues(Seq("10.0.0.0/16".asJson))
      )
    )

  private def mapSecurityGroupProperties(sg: SecurityGroup): Json =
    val baseProps = Json.obj()

    sg.properties.get("SecurityGroupIngress") match
      case Some(rules: List[_]) =>
        val azureRules =
          rules.asInstanceOf[List[Map[String, Any]]].zipWithIndex.map {
            case (rule, index) =>
              Json.obj(
                "name" -> s"AllowInbound${index + 1}".asJson,
                "properties" -> Json.obj(
                  "priority" -> (1000 + index * 10).asJson,
                  "direction" -> "Inbound".asJson,
                  "access" -> "Allow".asJson,
                  "protocol" -> rule("IpProtocol").toString.capitalize.asJson,
                  "sourcePortRange" -> "*".asJson,
                  "destinationPortRange" -> rule("FromPort").toString.asJson,
                  "sourceAddressPrefix" -> rule("CidrIp").toString.asJson,
                  "destinationAddressPrefix" -> "*".asJson
                )
              )
          }
        baseProps.deepMerge(
          Json.obj("securityRules" -> Json.fromValues(azureRules))
        )
      case None => baseProps

  private def mapLoadBalancerProperties(alb: ApplicationLoadBalancer): Json =
    val baseProps = Json.obj(
      "sku" -> Json.obj("name" -> "Standard".asJson),
      "frontendIPConfigurations" -> Json.fromValues(
        Seq(
          Json.obj(
            "name" -> "LoadBalancerFrontEnd".asJson,
            "properties" -> Json.obj(
              "publicIPAddress" -> Json.obj(
                "id" -> "[resourceId('Microsoft.Network/publicIPAddresses', 'myPublicIP')]".asJson
              )
            )
          )
        )
      )
    )

    alb.properties.get("Targets") match
      case Some(targets: List[_]) =>
        baseProps.deepMerge(
          Json.obj(
            "backendAddressPools" -> Json.fromValues(
              Seq(
                Json.obj(
                  "name" -> "BackendPool".asJson,
                  "properties" -> Json.obj(
                    "loadBalancerBackendAddresses" -> Json.fromValues(
                      targets
                        .asInstanceOf[List[String]]
                        .map(target => Json.obj("name" -> target.asJson))
                    )
                  )
                )
              )
            )
          )
        )
      case None => baseProps
