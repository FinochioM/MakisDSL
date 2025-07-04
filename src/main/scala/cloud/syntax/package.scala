package cloud.syntax

import cloud.*
import cloud.builder.*
import cloud.validation.*
import cloud.macros.EnvMacro.{env, env as envWithDefault}

export cloud.macros.EnvMacro.{env}

extension [T <: CloudResource](resource: T)(using builder: CloudAppBuilder)
  def dependsOn(dependencies: CloudResource*): T =
    val updated = resource match
      case s: ObjectStorage =>
        s.copy(dependencies = dependencies.toList).asInstanceOf[T]
      case f: ServerlessFunction =>
        f.copy(dependencies = dependencies.toList).asInstanceOf[T]
      case t: NoSqlTable =>
        t.copy(dependencies = dependencies.toList).asInstanceOf[T]
    builder.addResource(updated)
    updated

extension (storage: ObjectStorage)(using builder: CloudAppBuilder)
  def withVersioning(enabled: Boolean = true): ObjectStorage =
    val updated = storage.copy(properties =
      storage.properties + ("VersioningConfiguration" -> Map(
        "Status" -> (if (enabled) "Enabled" else "Suspended")
      ))
    )
    builder.addResource(updated)
    updated

  def withPublicAccess(blocked: Boolean = true): ObjectStorage =
    val updated = storage.copy(properties =
      storage.properties + ("PublicAccessBlockConfiguration" -> Map(
        "BlockPublicAcls" -> blocked,
        "BlockPublicPolicy" -> blocked,
        "IgnorePublicAcls" -> blocked,
        "RestrictPublicBuckets" -> blocked
      ))
    )
    builder.addResource(updated)
    updated

extension (
    builder: ServerlessFunctionBuilder[RuntimeSet, HandlerSet]
)(using cloudBuilder: CloudAppBuilder)
  def build: ServerlessFunction =
    val resource = ServerlessFunction(
      builder.name,
      builder.properties,
      builder.dependencies
    )
    cloudBuilder.addResource(resource)
    resource

extension (
    builder: NoSqlTableBuilder[HashKeySet]
)(using cloudBuilder: CloudAppBuilder)
  def build: NoSqlTable =
    val resource = NoSqlTable(
      builder.name,
      builder.properties,
      builder.dependencies
    )
    cloudBuilder.addResource(resource)
    resource

extension (sg: SecurityGroup)(using builder: CloudAppBuilder)
  def allowInbound(protocol: String, port: Int): SecurityGroup =
    val currentRules = sg.properties
      .getOrElse("SecurityGroupIngress", List.empty)
      .asInstanceOf[List[Map[String, Any]]]
    val newRule = Map(
      "IpProtocol" -> protocol.toLowerCase,
      "FromPort" -> port,
      "ToPort" -> port,
      "CidrIp" -> "0.0.0.0/0"
    )
    val updated = sg.copy(properties =
      sg.properties + ("SecurityGroupIngress" -> (currentRules :+ newRule))
    )
    builder.addResource(updated)
    updated

extension (alb: ApplicationLoadBalancer)(using builder: CloudAppBuilder)
  def withTargets(targets: CloudResource*): ApplicationLoadBalancer =
    val updated = alb.copy(properties =
      alb.properties + ("Targets" -> targets.map(_.reference.name).toList)
    )
    builder.addResource(updated)
    updated
