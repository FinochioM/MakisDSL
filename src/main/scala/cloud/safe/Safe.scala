package cloud.safe

import cloud.*
import cloud.builder.*
import cloud.validation.*

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
