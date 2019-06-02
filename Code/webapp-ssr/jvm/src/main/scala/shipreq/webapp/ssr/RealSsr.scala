package shipreq.webapp.ssr

import japgolly.scalagraal._

object RealSsr {
  import GraalBoopickle._
  import GraalJs._
  import SsrSharedData._

  val setup: Expr[Unit] =
    Expr.stdlibCosequenceDiscard(List(
      Expr("window = {console: console, navigator: {userAgent:''}}"),
      Expr.requireFileOnClasspath("webapp-ssr-deps.js"),
      Expr.requireFileOnClasspath("webapp-ssr.js"),
    ))

  val setUrl: String => Expr[Unit] =
    Expr.compileFnCall1[String](SsrJsFunctionManifest.SetUrl)(_.void)

  val renderPublic: PublicInitData => Expr[String] =
    Expr.compileFnCall1[PublicInitData](SsrJsFunctionManifest.Public)(_.asString)

  val renderProjectSpaLoader: ProjectSpaLoaderData => Expr[String] =
    Expr.compileFnCall1[ProjectSpaLoaderData](SsrJsFunctionManifest.ProjectSpaLoader)(_.asString)
}
