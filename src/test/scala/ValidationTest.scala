class ValidationTest extends munit.FunSuite {
  test("compile-time validation enforces required properties") {
    import cloud.*
    import cloud.CloudProvider.*
    import cloud.syntax.*
    import cloud.providers.aws.CloudFormationGenerator

    val myApp = cloudApp(provider = AWS) {
      val bucket = objectStorage("my-data-bucket")
        .withVersioning(true)

      // MUST have runtime and handler before .build()
      val function = serverlessFunction("my-api-handler")
        .withRuntime("nodejs18.x") // Required
        .withHandler("index.handler") // Required
        .withCode(bucket.reference)
        .dependsOn(bucket)
        .build // compiles if both runtime and handler are set

      val table = noSqlTable("my-users-table")
        .withHashKey("userId", "S") // Required
        .dependsOn(function)
        .build // compiles if hash key is set
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
    import cloud.syntax.*

    val myApp = cloudApp(provider = AWS) {
      val bucket = objectStorage("simple-bucket")
    }

    assert(myApp.resources.size == 1)
    assert(myApp.resources.head.name == "simple-bucket")
  }

  test("runtime suggestions provide helpful feedback") {
    import cloud.errors.RuntimeSuggestions

    val suggestion = RuntimeSuggestions.suggest("nodejs17.x")
    assert(suggestion.contains("nodejs18.x"))
    assert(suggestion.contains("Did you mean"))

    val suggestion2 = RuntimeSuggestions.suggest("python4")
    assert(suggestion2.contains("python3.9"))
  }
}
