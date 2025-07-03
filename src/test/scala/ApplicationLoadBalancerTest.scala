class ApplicationLoadBalancerTest extends munit.FunSuite {
  import cloud.*
  import cloud.CloudProvider.*
  import cloud.syntax.*
  import io.circe.parser.*

  def createLoadBalancerApp(provider: CloudProvider) = {
    cloudApp(provider = provider) {
      val function = serverlessFunction("api-handler")
        .withRuntime("nodejs18.x")
        .withHandler("index.handler")
        .build

      val alb = applicationLoadBalancer("web-alb")
        .withTargets(function)
    }
  }

  test("ApplicationLoadBalancer generates AWS CloudFormation") {
    import cloud.providers.aws.CloudFormationGenerator

    val app = createLoadBalancerApp(AWS)
    val template = CloudFormationGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== AWS ApplicationLoadBalancer CloudFormation ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("AWS::ElasticLoadBalancingV2::LoadBalancer"))
    assert(jsonString.contains("AWS::Lambda::Function"))
    assert(jsonString.contains("web-alb"))
    assert(jsonString.contains("api-handler"))

    assert(jsonString.contains("Targets"))
    assert(jsonString.contains("api-handler"))
  }

  test("ApplicationLoadBalancer generates Azure ARM template") {
    import cloud.providers.azure.ARMGenerator

    val app = createLoadBalancerApp(Azure)
    val template = ARMGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== Azure ApplicationLoadBalancer ARM ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("Microsoft.Network/loadBalancers"))
    assert(jsonString.contains("Microsoft.Web/sites"))
    assert(jsonString.contains("web-alb"))
    assert(jsonString.contains("api-handler"))

    assert(jsonString.contains("frontendIPConfigurations"))
    assert(jsonString.contains("backendAddressPools"))
    assert(jsonString.contains("LoadBalancerFrontEnd"))
    assert(jsonString.contains("BackendPool"))
  }

  test("ApplicationLoadBalancer generates GCP Deployment Manager") {
    import cloud.providers.gcp.DeploymentManagerGenerator

    val app = createLoadBalancerApp(GCP)
    val template = DeploymentManagerGenerator.generate(app)
    val jsonString = template.spaces2

    println("=== GCP ApplicationLoadBalancer Deployment Manager ===")
    println(jsonString)

    val parsed = parse(jsonString)
    assert(parsed.isRight)

    assert(jsonString.contains("compute.v1.forwardingRule"))
    assert(jsonString.contains("cloudfunctions.v1.function"))
    assert(jsonString.contains("web-alb"))
    assert(jsonString.contains("api-handler"))

    assert(jsonString.contains("EXTERNAL"))
    assert(jsonString.contains("us-central1"))
    assert(jsonString.contains("backendServices"))
  }

  test("ApplicationLoadBalancer has correct properties with targets") {
    val app = createLoadBalancerApp(AWS)

    assert(app.resources.size == 2)

    val alb = app.resources
      .find(_.resourceType == "ApplicationLoadBalancer")
      .get
      .asInstanceOf[ApplicationLoadBalancer]
    assert(alb.name == "web-alb")
    assert(alb.resourceType == "ApplicationLoadBalancer")

    val targets = alb.properties("Targets").asInstanceOf[List[String]]
    assert(targets.contains("api-handler"))
  }
}
