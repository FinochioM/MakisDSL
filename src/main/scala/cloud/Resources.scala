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

case class VirtualNetwork(
    name: String,
    properties: CloudConfig = Map.empty,
    override val dependencies: List[CloudResource] = List.empty
) extends NetworkResource:
  def resourceType: String = "VirtualNetwork"

case class SecurityGroup(
    name: String,
    properties: CloudConfig = Map.empty,
    override val dependencies: List[CloudResource] = List.empty
) extends SecurityResource:
  def resourceType: String = "SecurityGroup"

case class ApplicationLoadBalancer(
    name: String,
    properties: CloudConfig = Map.empty,
    override val dependencies: List[CloudResource] = List.empty
) extends LoadBalancerResource:
  def resourceType: String = "ApplicationLoadBalancer"

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

def virtualNetwork(name: String)(using
    builder: CloudAppBuilder
): VirtualNetwork =
  val resource = VirtualNetwork(name)
  builder.addResource(resource)
  resource

def securityGroup(name: String)(using builder: CloudAppBuilder): SecurityGroup =
  val resource = SecurityGroup(name)
  builder.addResource(resource)
  resource

def applicationLoadBalancer(name: String)(using
    builder: CloudAppBuilder
): ApplicationLoadBalancer =
  val resource = ApplicationLoadBalancer(name)
  builder.addResource(resource)
  resource
