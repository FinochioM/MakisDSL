package cloud.builder

import cloud.*
import cloud.validation.*

case class ServerlessFunctionBuilder[R <: PropertyStatus, H <: PropertyStatus](
    name: String,
    properties: CloudConfig = Map.empty,
    dependencies: List[CloudResource] = List.empty
):
  def withRuntime(runtime: String): ServerlessFunctionBuilder[RuntimeSet, H] =
    this
      .copy(properties = properties + ("Runtime" -> runtime))
      .asInstanceOf[ServerlessFunctionBuilder[RuntimeSet, H]]
  def withHandler(handler: String): ServerlessFunctionBuilder[R, HandlerSet] =
    this
      .copy(properties = properties + ("Handler" -> handler))
      .asInstanceOf[ServerlessFunctionBuilder[R, HandlerSet]]

  def withCode(code: String): ServerlessFunctionBuilder[R, H] =
    this.copy(properties = properties + ("Code" -> Map("ZipFile" -> code)))

  def withCode(bucketRef: ResourceReference): ServerlessFunctionBuilder[R, H] =
    this.copy(properties =
      properties + ("Code" -> Map(
        "S3Bucket" -> Map("Ref" -> bucketRef.name)
      ))
    )

  def dependsOn(deps: CloudResource*): ServerlessFunctionBuilder[R, H] =
    this.copy(dependencies = deps.toList)

case class NoSqlTableBuilder[K <: PropertyStatus](
    name: String,
    properties: CloudConfig = Map.empty,
    dependencies: List[CloudResource] = List.empty
):
  def withHashKey(
      keyName: String,
      keyType: String = "S"
  ): NoSqlTableBuilder[HashKeySet] =
    val keySchema = List(Map("AttributeName" -> keyName, "KeyType" -> "HASH"))
    val attributeDefinitions = List(
      Map("AttributeName" -> keyName, "AttributeType" -> keyType)
    )
    this
      .copy(properties =
        properties +
          ("KeySchema" -> keySchema) +
          ("AttributeDefinitions" -> attributeDefinitions) +
          ("BillingMode" -> "PAY_PER_REQUEST")
      )
      .asInstanceOf[NoSqlTableBuilder[HashKeySet]]

  def dependsOn(deps: CloudResource*): NoSqlTableBuilder[K] =
    this.copy(dependencies = deps.toList)
