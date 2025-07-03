class ResourceDepTest extends munit.FunSuite {
  import cloud.*
  import cloud.CloudProvider.*
  import cloud.syntax.*
  import cloud.providers.aws.CloudFormationGenerator
  import io.circe.parser.*
  test("resources can depend on each other") {
    val myApp = cloudApp(provider = AWS) {
      val bucket = objectStorage("my-code-bucket")
        .withVersioning(true)

      val function = serverlessFunction("my-api-handler")
        .withRuntime("nodejs18.x")
        .withHandler("index.handler")
        .withCode(bucket.reference)
        .dependsOn(bucket)
        .build

      val table = noSqlTable("my-users-table")
        .withHashKey("userId", "S")
        .dependsOn(function)
        .build
    }

    val cfTemplate = CloudFormationGenerator.generate(myApp)
    val jsonString = cfTemplate.spaces2

    println("Generated CloudFormation with Dependencies:")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("DependsOn"))
    assert(jsonString.contains("my-code-bucket"))
    assert(jsonString.contains("S3Bucket"))
    assert(jsonString.contains("Ref"))
  }
}
