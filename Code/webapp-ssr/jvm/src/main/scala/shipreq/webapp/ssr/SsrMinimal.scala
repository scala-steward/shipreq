package shipreq.webapp.ssr

import com.typesafe.scalalogging.StrictLogging
import japgolly.scalagraal._
import japgolly.scalagraal.GraalJs._
import shipreq.base.util.{Permission, Template, Url}
import shipreq.webapp.base.user.Username
import scalaz.std.either._
import scalaz.Applicative
import shipreq.webapp.base.data.Project
import shipreq.webapp.ssr.SsrAlgebra.Html

final class SsrMinimal[F[_]](implicit F: Applicative[F]) extends SsrAlgebra[F] with StrictLogging {
  import SsrSharedData._

  private[this] val fxNone: F[Option[Html]] = F.pure(None)

  private def baseUrl = Url.Absolute.Base("https://shipreq.com")

  private implicit def templateParamUsername: Template.Param[Username] =
    Template.Param(Username.apply)(_.value)

  private def withCtx[A](f: ContextSync => A): F[A] =
    F.point {
      val ctx = ContextSync()
      try {
        ctx.eval(RealSsr.setup)
        f(ctx)
      } finally
        ctx.close()
    }

  override def warmup =
    F.pure(())

  override def public(p: Permission): F[(Url.Relative, Option[Username]) => F[Option[Html]]] =
    withCtx { ctx =>

      def renderSome(u: Username) =
        ctx.eval(RealSsr.renderPublic(PublicInitData(p, Some(u))))

      val templates =
        for {
          _    <- ctx.eval(RealSsr.setUrl(baseUrl.value))
          anon <- ctx.eval(RealSsr.renderPublic(PublicInitData(p, None)))
          user <- Template.functor1A(renderSome)
        } yield (Html(anon), user.andThen(Html))

      templates match {
        case Right((anon, userT)) =>
          val fxAnon = F.pure(Option(anon))
          (url, username) =>
            if (url.isRoot)
              username.fold(fxAnon)(u => F.pure(Some(userT(u))))
            else
              fxNone

        case Left(e) =>
          logger.warn("Failed to pre-compile public spa SSR.", e)
          (_, _) => fxNone
      }
    }

  override def projectSpaLoader: F[ProjectSpaLoaderData => F[Option[Html]]] =
    withCtx { ctx =>

      def render(u: Username, p: Project.Name) =
        ctx.eval(RealSsr.renderProjectSpaLoader(ProjectSpaLoaderData(u, p)))

      Template.functor2A(render) match {
        case Right(t) =>
          i => F.pure(Some(Html(t(i.username, i.projectName))))
        case Left(e) =>
          logger.warn("Failed to pre-compile public spa SSR.", e)
          _ => fxNone
      }
    }
}
