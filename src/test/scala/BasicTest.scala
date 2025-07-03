class BasicDSLTest extends munit.FunSuite {
  test("basic DSL creates cloud app with resources") {
    import cloud.*
    import cloud.CloudProvider.*
    import cloud.syntax.*
    import cloud.providers.aws.CloudFormationGenerator
    import io.circe.parser.*

    val myApp = cloudApp(provider = AWS) {
      val storage = objectStorage("my-data-bucket")
        .withVersioning(true)
        .withPublicAccess(true)

      val function = serverlessFunction("my-api-handler")
        .withRuntime("nodejs18.x")
        .withHandler("index.handler")
        .withCode(
          "exports.handler = async (event) => { return { statusCode: 200, body: 'Hello World!' }; };"
        )

      val database = noSqlTable("my-users-table")
        .withHashKey("userId", "S")
    }

    val cfTemplate = CloudFormationGenerator.generate(myApp)
    val jsonString = cfTemplate.spaces2

    println("Generated CloudFormation Template:")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("VersioningConfiguration"))
    assert(jsonString.contains("PublicAccessBlockConfiguration"))
    assert(jsonString.contains("Runtime"))
    assert(jsonString.contains("nodejs18.x"))
    assert(jsonString.contains("Handler"))
    assert(jsonString.contains("KeySchema"))
    assert(jsonString.contains("AttributeDefinitions"))
    assert(jsonString.contains("PAY_PER_REQUEST"))
  }
}
