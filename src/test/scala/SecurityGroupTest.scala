class SecurityGroupTest extends munit.FunSuite {
  import cloud.*
  import cloud.CloudProvider.*
  import cloud.syntax.*
  import io.circe.parser.*

  def createSecurityGroupApp(provider: CloudProvider) = {
    cloudApp(provider = provider) {
      val sg = securityGroup("web-sg")
        .allowInbound("HTTP", 80)
        .allowInbound("HTTPS", 443)
    }
  }

  test("SecurityGroup generates AWS CloudFormation") {
    import cloud.providers.aws.CloudFormationGenerator

    val app = createSecurityGroupApp(AWS)
    val template = CloudFormationGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== AWS SecurityGroup CloudFormation ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("AWS::EC2::SecurityGroup"))
    assert(jsonString.contains("web-sg"))

    assert(jsonString.contains("SecurityGroupIngress"))
    assert(jsonString.contains("80"))
    assert(jsonString.contains("443"))
    assert(jsonString.contains("http"))
    assert(jsonString.contains("https"))
  }

  test("SecurityGroup generates Azure ARM template") {
    import cloud.providers.azure.ARMGenerator

    val app = createSecurityGroupApp(Azure)
    val template = ARMGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== Azure SecurityGroup ARM ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("Microsoft.Network/networkSecurityGroups"))
    assert(jsonString.contains("web-sg"))

    assert(jsonString.contains("securityRules"))
    assert(jsonString.contains("AllowInbound"))
    assert(jsonString.contains("Inbound"))
    assert(jsonString.contains("Allow"))
    assert(jsonString.contains("80"))
    assert(jsonString.contains("443"))
  }

  test("SecurityGroup generates GCP Deployment Manager") {
    import cloud.providers.gcp.DeploymentManagerGenerator

    val app = createSecurityGroupApp(GCP)
    val template = DeploymentManagerGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== GCP SecurityGroup Deployment Manager ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("compute.v1.firewall"))
    assert(jsonString.contains("web-sg"))

    assert(jsonString.contains("INGRESS"))
    assert(jsonString.contains("allowed"))
    assert(jsonString.contains("sourceRanges"))
    assert(jsonString.contains("0.0.0.0/0"))
    assert(jsonString.contains("80"))
    assert(jsonString.contains("443"))
  }

  test("SecurityGroup has correct properties after allowInbound") {
    val app = createSecurityGroupApp(AWS)

    assert(app.resources.size == 1)
    val sg = app.resources.head.asInstanceOf[SecurityGroup]
    assert(sg.name == "web-sg")
    assert(sg.resourceType == "SecurityGroup")

    val rules =
      sg.properties("SecurityGroupIngress").asInstanceOf[List[Map[String, Any]]]
    assert(rules.size == 2)
    assert(
      rules.exists(rule =>
        rule("FromPort") == 80 && rule("IpProtocol") == "http"
      )
    )
    assert(
      rules.exists(rule =>
        rule("FromPort") == 443 && rule("IpProtocol") == "https"
      )
    )
  }
}
