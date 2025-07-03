package cloud

trait CloudResource:
  def resourceType: String
  def name: String
  def properties: CloudConfig

trait StorageResource extends CloudResource
trait ComputeResource extends CloudResource
trait DatabaseResource extends CloudResource
