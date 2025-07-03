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
    case _: ObjectStorage      => "Microsoft.Storage/storageAccounts"
    case _: ServerlessFunction => "Microsoft.Web/sites"
    case _: NoSqlTable         => "Microsoft.DocumentDB/databaseAccounts"

  private def getApiVersion(resource: CloudResource): String = resource match
    case _: ObjectStorage      => "2023-01-01"
    case _: ServerlessFunction => "2023-01-01"
    case _: NoSqlTable         => "2023-04-15"

  private def mapProperties(resource: CloudResource): Json = resource match
    case storage: ObjectStorage       => mapStorageProperties(storage)
    case function: ServerlessFunction => mapFunctionProperties(function)
    case table: NoSqlTable            => mapTableProperties(table)

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
