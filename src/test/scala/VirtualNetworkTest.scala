class VirtualNetworkTest extends munit.FunSuite {
  import cloud.*
  import cloud.CloudProvider.*
  import cloud.syntax.*
  import io.circe.parser.*

  def createNetworkApp(provider: CloudProvider) = {
    cloudApp(provider = provider) {
      val network = virtualNetwork("my-vpc")
    }
  }

  test("VirtualNetwork generates AWS CloudFormation") {
    import cloud.providers.aws.CloudFormationGenerator

    val app = createNetworkApp(AWS)
    val template = CloudFormationGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== AWS VirtualNetwork CloudFormation ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("AWS::EC2::VPC"))
    assert(jsonString.contains("my-vpc"))

    assert(jsonString.contains("AWSTemplateFormatVersion"))
    assert(jsonString.contains("Resources"))
  }

  test("VirtualNetwork generates Azure ARM template") {
    import cloud.providers.azure.ARMGenerator

    val app = createNetworkApp(Azure)
    val template = ARMGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== Azure VirtualNetwork ARM ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("Microsoft.Network/virtualNetworks"))
    assert(jsonString.contains("my-vpc"))

    assert(jsonString.contains("$schema"))
    assert(jsonString.contains("contentVersion"))
    assert(jsonString.contains("addressSpace"))
    assert(jsonString.contains("10.0.0.0/16"))
  }

  test("VirtualNetwork generates GCP Deployment Manager") {
    import cloud.providers.gcp.DeploymentManagerGenerator

    val app = createNetworkApp(GCP)
    val template = DeploymentManagerGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== GCP VirtualNetwork Deployment Manager ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("compute.v1.network"))
    assert(jsonString.contains("my-vpc"))

    assert(jsonString.contains("autoCreateSubnetworks"))
    assert(jsonString.contains("routingConfig"))
    assert(jsonString.contains("REGIONAL"))
  }

  test("VirtualNetwork has correct resource properties") {
    val app = createNetworkApp(AWS)

    assert(app.resources.size == 1)
    assert(app.resources.head.name == "my-vpc")
    assert(app.resources.head.resourceType == "VirtualNetwork")
    assert(app.resources.head.isInstanceOf[VirtualNetwork])
  }
}
