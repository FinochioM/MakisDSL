package cloud

trait CloudResource:
  def resourceType: String
  def name: String
  def properties: CloudConfig
  def dependencies: List[CloudResource] = List.empty

  def reference: ResourceReference = ResourceReference(this)

trait StorageResource extends CloudResource
trait ComputeResource extends CloudResource
trait DatabaseResource extends CloudResource

case class ResourceReference(resource: CloudResource):
  def name: String = resource.name
  def resourceType: String = resource.resourceType
