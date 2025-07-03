package cloud

case class CloudAppBuilder(val provider: CloudProvider):
  private val _resources = scala.collection.mutable.ListBuffer[CloudResource]()

  def resources: List[CloudResource] = _resources.toList

  def addResource(resource: CloudResource): Unit =
    _resources += resource
