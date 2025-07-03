class ValidationTest extends munit.FunSuite {
  import cloud.*
  import cloud.CloudProvider.*
  import cloud.safe.{objectStorage, serverlessFunction, noSqlTable}
  import cloud.providers.aws.CloudFormationGenerator
  test("compile-time validation enforces required properties") {
    val myApp = cloudApp(provider = AWS) {
      val bucket = objectStorage("my-data-bucket")
        .withVersioning(true)

      // MUST have runtime and handler before .build()
      val function = serverlessFunction("my-api-handler")
        .withRuntime("nodejs18.x") // required
        .withHandler("index.handler") // required
        .withCode(bucket.reference)
        .dependsOn(bucket)
        .build // only compiles if both runtime and handler are set

      val table = noSqlTable("my-users-table")
        .withHashKey("userId", "S") // required
        .dependsOn(function)
        .build
    }

    val cfTemplate = CloudFormationGenerator.generate(myApp)
    val jsonString = cfTemplate.spaces2

    assert(jsonString.contains("Runtime"))
    assert(jsonString.contains("Handler"))
    assert(jsonString.contains("KeySchema"))
  }

  test("ObjectStorage works without required properties") {
    import cloud.*
    import cloud.CloudProvider.*
    import cloud.safe.*

    val myApp = cloudApp(provider = AWS) {
      val bucket = objectStorage("simple-bucket")
    }

    assert(myApp.resources.size == 1)
    assert(myApp.resources.head.name == "simple-bucket")
  }
}
