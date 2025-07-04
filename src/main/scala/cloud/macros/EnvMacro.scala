package cloud.macros

import scala.quoted.*

object EnvMacro:
  inline def env(key: String): String = ${ envImpl('key) }
  inline def env(key: String, default: String): String = ${
    envWithDefaultImpl('key, 'default)
  }

  private def envImpl(key: Expr[String])(using Quotes): Expr[String] =
    import quotes.reflect.*

    key.value match
      case Some(keyStr) =>
        val value =
          sys.env.get(keyStr).orElse(Option(System.getProperty(keyStr)))
        value match
          case Some(v) => Expr(v)
          case None =>
            report.errorAndAbort(
              s"Environment variable '$keyStr' not found. Use env(key, default) for optional vars."
            )
      case None =>
        report.errorAndAbort(
          "Environment variable key must be a compile-time constant"
        )

  private def envWithDefaultImpl(key: Expr[String], default: Expr[String])(using
      Quotes
  ): Expr[String] =
    import quotes.reflect.*

    (key.value, default.value) match
      case (Some(keyStr), Some(defaultStr)) =>
        val value = sys.env
          .get(keyStr)
          .orElse(Option(System.getProperty(keyStr)))
          .getOrElse(defaultStr)
        Expr(value)
      case (None, _) =>
        report.errorAndAbort(
          "Environment variable key must be a compile-time constant"
        )
      case (_, None) =>
        report.errorAndAbort("Default value must be a compile-time constant")
