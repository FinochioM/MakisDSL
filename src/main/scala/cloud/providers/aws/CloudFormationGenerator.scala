package cloud.providers.aws

import cloud.*
import io.circe.*
import io.circe.syntax.*

object CloudFormationGenerator:
  given Encoder[CloudConfig] = Encoder.instance { config =>
    Json.obj(
      config.map { case (key, value) =>
        key -> encodeValue(value)
      }.toSeq*
    )
  }

  private def encodeValue(value: Any): Json = value match
    case s: String  => s.asJson
    case i: Int     => i.asJson
    case d: Double  => d.asJson
    case b: Boolean => b.asJson
    case l: List[_] =>
      Json.fromValues(l.map(encodeValue))
    case m: Map[_, _] =>
      Json.obj(
        m.asInstanceOf[Map[String, Any]]
          .map((k, v) => k -> encodeValue(v))
          .toSeq*
      )
    case _ => value.toString.asJson

  def generate(app: CloudApp): Json =
    Json.obj(
      "AWSTemplateFormatVersion" -> "2010-09-09".asJson,
      "Resources" -> Json.obj(
        app.resources.map(generateResource)*
      )
    )

  private def generateResource(resource: CloudResource): (String, Json) =
    val baseResource = Json.obj(
      "Type" -> mapResourceType(resource).asJson,
      "Properties" -> resource.properties.asJson
    )

    val resourceWithDeps = if (resource.dependencies.nonEmpty) {
      baseResource.deepMerge(
        Json.obj(
          "DependsOn" -> resource.dependencies.map(_.name).asJson
        )
      )
    } else {
      baseResource
    }

    resource.name -> resourceWithDeps

  private def mapResourceType(resource: CloudResource): String = resource match
    case _: ObjectStorage      => "AWS::S3::Bucket"
    case _: ServerlessFunction => "AWS::Lambda::Function"
    case _: NoSqlTable         => "AWS::DynamoDB::Table"
    case _: VirtualNetwork     => "AWS::EC2::VPC"
    case _: SecurityGroup      => "AWS::EC2::SecurityGroup"
    case _: ApplicationLoadBalancer =>
      "AWS::ElasticLoadBalancingV2::LoadBalancer"
