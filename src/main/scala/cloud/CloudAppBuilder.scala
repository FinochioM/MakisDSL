package cloud

case class CloudAppBuilder(val provider: CloudProvider):
  private val _resources =
    scala.collection.mutable.Map[String, CloudResource]()

  def resources: List[CloudResource] = _resources.values.toList

  def addResource(resource: CloudResource): Unit =
    _resources(resource.name) = resource
