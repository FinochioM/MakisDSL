package object cloud {
  enum CloudProvider:
    case AWS, Azure, GCP

  type CloudConfig = Map[String, Any]
}
