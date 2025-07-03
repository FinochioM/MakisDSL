package cloud.syntax

import cloud.*

extension (storage: ObjectStorage)(using builder: CloudAppBuilder)
  def withVersioning(enabled: Boolean = true): ObjectStorage =
    val updated = storage.copy(properties =
      storage.properties + ("VersioningConfiguration" -> Map(
        "Status" -> (if (enabled) "Enabled" else "Suspended")
      ))
    )
    builder.addResource(updated)
    updated

  def withPublicAccess(blocked: Boolean = true): ObjectStorage =
    val updated = storage.copy(properties =
      storage.properties + ("PublicAccessBlockConfiguration" -> Map(
        "BlockPublicAcls" -> blocked,
        "BlockPublicPolicy" -> blocked,
        "IgnorePublicAcls" -> blocked,
        "RestrictPublicBuckets" -> blocked
      ))
    )
    builder.addResource(updated)
    updated

extension (function: ServerlessFunction)(using builder: CloudAppBuilder)
  def withRuntime(runtime: String): ServerlessFunction =
    val updated =
      function.copy(properties = function.properties + ("Runtime" -> runtime))
    builder.addResource(updated)
    updated

  def withHandler(handler: String): ServerlessFunction =
    val updated =
      function.copy(properties = function.properties + ("Handler" -> handler))
    builder.addResource(updated)
    updated

  def withCode(zipFile: String): ServerlessFunction =
    val updated = function.copy(properties =
      function.properties + ("Code" -> Map("ZipFile" -> zipFile))
    )
    builder.addResource(updated)
    updated

extension (table: NoSqlTable)(using builder: CloudAppBuilder)
  def withHashKey(keyName: String, keyType: String = "S"): NoSqlTable =
    val keySchema = List(Map("AttributeName" -> keyName, "KeyType" -> "HASH"))
    val attributeDefinitions = List(
      Map("AttributeName" -> keyName, "AttributeType" -> keyType)
    )
    val updated = table.copy(properties =
      table.properties +
        ("KeySchema" -> keySchema) +
        ("AttributeDefinitions" -> attributeDefinitions) +
        ("BillingMode" -> "PAY_PER_REQUEST")
    )
    builder.addResource(updated)
    updated
