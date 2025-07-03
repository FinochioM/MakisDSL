class TriCloudTest extends munit.FunSuite {
  import cloud.*
  import cloud.syntax.*

  def createApp(provider: CloudProvider) = {
    cloudApp(provider = provider) {
      val bucket = objectStorage("my-data-bucket")
        .withVersioning(true)

      val function = serverlessFunction("my-api-handler")
        .withRuntime("nodejs18.x")
        .withHandler("index.handler")
        .withCode(bucket.reference)
        .dependsOn(bucket)

      val table = noSqlTable("my-users-table")
        .withHashKey("userId", "S")
        .dependsOn(function)
    }
  }

  test("same DSL generates AWS CloudFormation") {
    import cloud.*
    import cloud.CloudProvider.*
    import cloud.providers.aws.CloudFormationGenerator
    import io.circe.parser.*

    val app = createApp(AWS)
    val template = CloudFormationGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== AWS CloudFormation Template ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("AWS::S3::Bucket"))
    assert(jsonString.contains("AWS::Lambda::Function"))
    assert(jsonString.contains("AWS::DynamoDB::Table"))

    assert(jsonString.contains("DependsOn"))

    assert(jsonString.contains("\"S3Bucket\" : {"))
    assert(jsonString.contains("\"Ref\" : \"my-data-bucket\""))
    assert(!jsonString.contains("Map(Ref -> my-data-bucket)"))
  }

  test("same DSL generates Azure ARM template") {
    import cloud.*
    import cloud.CloudProvider.*
    import cloud.providers.azure.ARMGenerator
    import io.circe.parser.*

    val app = createApp(Azure)
    val template = ARMGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== Azure ARM Template ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("Microsoft.Storage/storageAccounts"))
    assert(jsonString.contains("Microsoft.Web/sites"))
    assert(jsonString.contains("Microsoft.DocumentDB/databaseAccounts"))

    assert(jsonString.contains("isVersioningEnabled"))
  }

  test("same DSL generates GCP Deployment Manager template") {
    import cloud.*
    import cloud.CloudProvider.*
    import cloud.providers.gcp.DeploymentManagerGenerator
    import io.circe.parser.*

    val app = createApp(GCP)
    val template = DeploymentManagerGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== GCP Deployment Manager Template ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("storage.v1.bucket"))
    assert(jsonString.contains("cloudfunctions.v1.function"))
    assert(jsonString.contains("firestore.v1.database"))

    assert(jsonString.contains("location"))
    assert(jsonString.contains("us-central1"))
  }

  test("all three providers generate different templates from same DSL") {
    import cloud.*
    import cloud.CloudProvider.*
    import cloud.providers.aws.CloudFormationGenerator
    import cloud.providers.azure.ARMGenerator
    import cloud.providers.gcp.DeploymentManagerGenerator

    val awsApp = createApp(AWS)
    val azureApp = createApp(Azure)
    val gcpApp = createApp(GCP)

    val awsTemplate = CloudFormationGenerator.generate(awsApp).spaces2
    val azureTemplate = ARMGenerator.generate(azureApp).spaces2
    val gcpTemplate = DeploymentManagerGenerator.generate(gcpApp).spaces2

    assert(awsTemplate != azureTemplate)
    assert(azureTemplate != gcpTemplate)
    assert(awsTemplate != gcpTemplate)

    assert(awsTemplate.contains("my-data-bucket"))
    assert(azureTemplate.contains("my-data-bucket"))
    assert(gcpTemplate.contains("my-data-bucket"))

    assert(awsTemplate.contains("my-api-handler"))
    assert(azureTemplate.contains("my-api-handler"))
    assert(gcpTemplate.contains("my-api-handler"))

    assert(awsTemplate.contains("my-users-table"))
    assert(azureTemplate.contains("my-users-table"))
    assert(gcpTemplate.contains("my-users-table"))
  }
}
