class CloudDslTest extends munit.FunSuite {
  test("basic DSL creates cloud app with resources") {
    import cloud.*
    import cloud.CloudProvider.*
    import cloud.providers.aws.CloudFormationGenerator
    import io.circe.parser.*

    val myApp = cloudApp(provider = AWS) {
      val storage = objectStorage("my-bucket")
      val functionn = serverlessFunction("my-handler")
    }

    val cfTemplate = CloudFormationGenerator.generate(myApp)
    val jsonString = cfTemplate.spaces2

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("AWSTemplateFormatVersion"))
    assert(jsonString.contains("AWS::S3::Bucket"))
    assert(jsonString.contains("AWS::Lambda::Function"))
    assert(jsonString.contains("my-bucket"))
    assert(jsonString.contains("my-handler"))
  }
}
