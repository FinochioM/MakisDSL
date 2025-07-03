package cloud

import cloud.builder.*
import cloud.validation.*

case class ObjectStorage(
    name: String,
    properties: CloudConfig = Map.empty,
    override val dependencies: List[CloudResource] = List.empty
) extends StorageResource:
  def resourceType: String = "ObjectStorage"

case class ServerlessFunction(
    name: String,
    properties: CloudConfig = Map.empty,
    override val dependencies: List[CloudResource] = List.empty
) extends ComputeResource:
  def resourceType: String = "ServerlessFunction"

case class NoSqlTable(
    name: String,
    properties: CloudConfig = Map.empty,
    override val dependencies: List[CloudResource] = List.empty
) extends DatabaseResource:
  def resourceType: String = "NoSqlTable"

def objectStorage(name: String)(using builder: CloudAppBuilder): ObjectStorage =
  val resource = ObjectStorage(name)
  builder.addResource(resource)
  resource

def serverlessFunction(name: String)(using
    builder: CloudAppBuilder
): ServerlessFunctionBuilder[RuntimeUnset, HandlerUnset] =
  ServerlessFunctionBuilder[RuntimeUnset, HandlerUnset](name)

def noSqlTable(name: String)(using
    builder: CloudAppBuilder
): NoSqlTableBuilder[HashKeyUnset] =
  NoSqlTableBuilder[HashKeyUnset](name)
