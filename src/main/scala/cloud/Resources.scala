package cloud

case class ObjectStorage(name: String, properties: CloudConfig = Map.empty)
    extends StorageResource:
  def resourceType: String = "ObjectStorage"

case class ServerlessFunction(name: String, properties: CloudConfig = Map.empty)
    extends ComputeResource:
  def resourceType: String = "ServerlessFunction"

case class NoSqlTable(name: String, properties: CloudConfig = Map.empty)
    extends DatabaseResource:
  def resourceType: String = "NoSqlTable"

def objectStorage(name: String)(using builder: CloudAppBuilder): ObjectStorage =
  val resource = ObjectStorage(name)
  builder.addResource(resource)
  resource

def serverlessFunction(name: String)(using
    builder: CloudAppBuilder
): ServerlessFunction =
  val resource = ServerlessFunction(name)
  builder.addResource(resource)
  resource

def noSqlTable(name: String)(using builder: CloudAppBuilder): NoSqlTable =
  val resource = NoSqlTable(name)
  builder.addResource(resource)
  resource
