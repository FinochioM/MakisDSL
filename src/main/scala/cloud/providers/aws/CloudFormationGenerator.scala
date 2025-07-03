package cloud.providers.aws

import cloud.*
import io.circe.*
import io.circe.syntax.*

object CloudFormationGenerator:
  given Encoder[CloudConfig] = Encoder.instance { config =>
    Json.obj(
      config.map { case (key, value) =>
        key -> (value match
          case s: String  => s.asJson
          case i: Int     => i.asJson
          case d: Double  => d.asJson
          case b: Boolean => b.asJson
          case l: List[_] => Json.fromValues(l.map(_.toString.asJson))
          case m: Map[_, _] =>
            Json.fromFields(
              m.asInstanceOf[Map[String, Any]]
                .map((k, v) => k -> v.toString.asJson)
            )
          case _ => value.toString.asJson
        )
      }.toSeq: _*
    )
  }

  def generate(app: CloudApp): Json =
    Json.obj(
      "AWSTemplateFormatVersion" -> "2010-09-09".asJson,
      "Resources" -> Json.obj(
        app.resources.map(generateResource): _*
      )
    )

  private def generateResource(resource: CloudResource): (String, Json) =
    resource.name -> Json.obj(
      "Type" -> mapResourceType(resource).asJson,
      "Properties" -> resource.properties.asJson
    )

  private def mapResourceType(resource: CloudResource): String = resource match
    case _: ObjectStorage      => "AWS::S3::Bucket"
    case _: ServerlessFunction => "AWS::Lambda::Function"
    case _: NoSqlTable         => "AWS::DynamoDB::Table"
