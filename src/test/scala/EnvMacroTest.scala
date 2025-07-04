class EnvMacroTest extends munit.FunSuite {
  import cloud.*
  import cloud.CloudProvider.*
  import cloud.syntax.*
  import cloud.providers.aws.CloudFormationGenerator
  import io.circe.parser.*

  test("env macro reads environment variables at compile time") {
    val myApp = cloudApp(provider = AWS) {
      // created a variable with SET command on windows and works.
      val storage = objectStorage(env("TEST_BUCKET_NAME", "default-bucket"))
        .withVersioning(true)

      val function =
        serverlessFunction(s"${env("TEST_ENV", "dev")}-api-handler")
          .withRuntime("nodejs18.x")
          .withHandler("index.handler")
          .withCode("console.log('Hello from ' + process.env.NODE_ENV)")
          .build

      val table = noSqlTable(s"${env("TEST_ENV", "dev")}-users-table")
        .withHashKey("userId", "S")
        .build
    }

    val cfTemplate = CloudFormationGenerator.generate(myApp)
    val jsonString = cfTemplate.spaces2

    println("Generated CloudFormation with env vars:")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("my-prod-bucket"))
    assert(jsonString.contains("dev-api-handler"))
    assert(jsonString.contains("dev-users-table"))

    assert(jsonString.contains("AWS::S3::Bucket"))
    assert(jsonString.contains("AWS::Lambda::Function"))
    assert(jsonString.contains("AWS::DynamoDB::Table"))
  }

  test("env macro with missing variable and no default fails at compile time") {
    // this test shows compile-time safety
    // uncomment the line below to see compile error:
    // val badName = env("NONEXISTENT_VAR") // should fail at compile time

    // this works with default:
    val goodName = env("NONEXISTENT_VAR", "fallback-name")
    assert(goodName == "fallback-name")
  }
}
