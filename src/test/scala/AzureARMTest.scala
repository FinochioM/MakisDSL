class AzureARMTest extends munit.FunSuite {
  test("generates Azure ARM template from same DSL") {
    import cloud.*
    import cloud.CloudProvider.*
    import cloud.syntax.*
    import cloud.providers.azure.ARMGenerator
    import io.circe.parser.*

    val myApp = cloudApp(provider = Azure) {
      val storage = objectStorage("mystorageaccount")
        .withVersioning(true)

      val function = serverlessFunction("my-function-app")
        .withRuntime("nodejs18.x")

      val database = noSqlTable("my-cosmos-db")
        .withHashKey("id", "S")
    }

    val armTemplate = ARMGenerator.generate(myApp)
    val jsonString = armTemplate.spaces2

    println("Generated Azure ARM Template:")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("Microsoft.Storage/storageAccounts"))
    assert(jsonString.contains("Microsoft.Web/sites"))
    assert(jsonString.contains("Microsoft.DocumentDB/databaseAccounts"))
    assert(jsonString.contains("isVersioningEnabled"))
  }
}
