package cloud.errors

import scala.annotation.compileTimeOnly
import cloud.builder.*
import cloud.validation.*

extension [R <: PropertyStatus, H <: PropertyStatus](
    builder: ServerlessFunctionBuilder[R, H]
)
  @compileTimeOnly(
    "Lambda function requires Runtime. Add .withRuntime(\"nodejs18.x\") before .build()"
  )
  def buildMissingRuntime(using evidence: R =:= RuntimeUnset): Nothing = ???

  @compileTimeOnly(
    "Lambda function requires Handler. Add .withHandler(\"index.handler\") before .build()"
  )
  def buildMissingHandler(using evidence: H =:= HandlerUnset): Nothing = ???

  @compileTimeOnly(
    "Lambda function is missing required properties. Add .withRuntime() and .withHandler() before .build()"
  )
  def buildMissingBoth(using
      evidence1: R =:= RuntimeUnset,
      evidence2: H =:= HandlerUnset
  ): Nothing = ???

extension [K <: PropertyStatus](builder: NoSqlTableBuilder[K])
  @compileTimeOnly(
    "NoSQL table requires Hash Key. Add .withHashKey(\"id\", \"S\") before .build()"
  )
  def buildMissingHashKey(using evidence: K =:= HashKeyUnset): Nothing = ???

object RuntimeSuggestions:
  val validRuntimes = List(
    "nodejs18.x",
    "nodejs16.x",
    "nodejs14.x",
    "python3.9",
    "python3.8",
    "python3.7",
    "java17",
    "java11",
    "java8",
    "dotnet6",
    "dotnet5",
    "dotnetcore3.1",
    "go1.x",
    "ruby2.7"
  )

  def suggest(invalid: String): String =
    val closest = validRuntimes.minBy(valid =>
      levenshteinDistance(invalid.toLowerCase, valid.toLowerCase)
    )
    s"Invalid runtime '$invalid'. Did you mean '$closest'? Valid options: ${validRuntimes.mkString(", ")}"

  private def levenshteinDistance(s1: String, s2: String): Int =
    val memo = Array.tabulate(s1.length + 1, s2.length + 1)((i, j) =>
      if (i == 0) j else if (j == 0) i else 0
    )

    for (i <- 1 to s1.length; j <- 1 to s2.length) {
      memo(i)(j) = (memo(i - 1)(j) + 1)
        .min(memo(i)(j - 1) + 1)
        .min(memo(i - 1)(j - 1) + (if (s1(i - 1) == s2(j - 1)) 0 else 1))
    }
    memo(s1.length)(s2.length)
