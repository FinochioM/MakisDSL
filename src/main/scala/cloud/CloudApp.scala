package cloud

case class CloudApp(
    provider: CloudProvider,
    resources: List[CloudResource] = List.empty
)

def cloudApp(provider: CloudProvider)(
    block: CloudAppBuilder ?=> Unit
): CloudApp =
  given builder: CloudAppBuilder = CloudAppBuilder(provider)
  block
  CloudApp(provider, builder.resources.toList)
