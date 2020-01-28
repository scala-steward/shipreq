package shipreq.webapp.server.logic.dispatch

import japgolly.microlibs.nonempty.NonEmptySet
import japgolly.microlibs.utils.Utils
import japgolly.univeq._
import scalaz.{-\/, Monad, \/, \/-}
import scalaz.syntax.monad._
import shipreq.base.util.{?=>, FnWithFallback, Url}
import shipreq.webapp.server.logic.{MetricsLogic, TraceLogic}

final class RouteBuilderModule[F[_], RealReq, RealRes](makeRealRes: (RealReq, Response) => F[RealRes])
                                                      (implicit F: Monad[F],
                                                       metrics   : MetricsLogic[F],
                                                       val tracer: TraceLogic[F, RealReq, RealRes]) {

  type Req = Request[RealReq]

  case class Respond(responder: Req => F[RealRes]) extends RouteBuilder.Complete {

    final override def build: Req ?=> F[RealRes] =
      FnWithFallback[Req, F[RealRes]](_ => responder)
  }
  
  object Respond {
//    private def makeReal(responder: Req => F[Response]): Req => F[RealRes] =
//      req => responder(req).flatMap(res => makeRealRes(req.real, res))

    def pure(r: ResponseCmd): Respond =
      ??? // const(F.point(r))

    def pure(r: Response): Respond =
      ??? // const(F.point(r))

    def const(f: F[Response]): Respond =
      ??? // apply(_ => f)

    def fn(f: Req => Response): Respond =
      ??? // apply(f.andThen(F.point(_)))
  }

  sealed trait RouteBuilder {
    import RouteBuilder.Internals._

    type Self <: RouteBuilder

    protected[RouteBuilderModule] def whenW(w: When): Self

    def |(orElse: RouteBuilder.Complete): Self

    def ensure(f: Req => Boolean, whenFalse: Respond): Self

    final def unlessF(f: Req => F[Boolean]): Self =
      whenF(f(_).map(!_))

    final def unless(f: Req => Boolean): Self =
      when(!f(_))

    final def whenF(f: Req => F[Boolean]): Self =
      whenW(When.effect(f))

    final def when(f: Req => Boolean): Self =
      whenW(When.pure(f))

    final def requireMethod(m: Method): Self =
      ensure(_.method eq m, Respond pure ResponseCmd.StatusOnly.MethodNotAllowed.withoutCookieUpdate)

    final def whenUrlIs(url: Url.Relative): Self =
      when(_.path ==* url)

    final def whenUrlIsAnyOf(urls: NonEmptySet[Url.Relative]): Self = {
      val norm: Url.Relative => String = u => Url.dropTailSlashes(u.underlying)
      val lookup = Utils.quickStringExists(urls.whole.map(norm))
      when(r => lookup(norm(r.path)))
    }
  }

  object RouteBuilder {
    import Internals._

    sealed trait Incomplete[R] extends RouteBuilder {
      final override type Self = Incomplete[R]

      protected[RouteBuilderModule] def respondUsing: R => Respond

      final override protected[RouteBuilderModule] def whenW(w: When): Self =
        IncompleteWhen(w, this)

      final override def |(orElse: RouteBuilder.Complete): Self =
        IncompleteOrElse(this, orElse)

      final override def ensure(f: Req => Boolean, whenFalse: Respond): Self =
        when(f) | respond_(whenFalse)

      final def traceUrlWithSpan(url: Url.Relative)(implicit ev: Incomplete[R] =:= Incomplete[Respond]): Incomplete[tracer.Span => Respond] = {
        val self = ev(this)
        val respondUsing = self.respondUsing
        self.contrafmap { main =>
          metrics.setHttpName(url.relativeUrl).map { _ =>
            Respond { req =>
              tracer.http(url.relativeUrl, req.real, req.path) { span =>
                respondUsing(main(span)).responder(req)
              }
            }
          }
        }
      }

      final def traceUrl(url: Url.Relative)(implicit ev: Incomplete[R] =:= Incomplete[Respond]): Incomplete[Respond] =
        traceUrlWithSpan(url).contramap(r => _ => r)

      final def getWithSpan(url: Url.Relative)(implicit ev: Incomplete[R] =:= Incomplete[Respond]): Incomplete[tracer.Span => Respond] =
        requireMethod(Method.Get)
          .traceUrlWithSpan(url)
          .whenUrlIs(url)

      final def get(url: Url.Relative)(implicit ev: Incomplete[R] =:= Incomplete[Respond]): Incomplete[Respond] =
        getWithSpan(url).contramap(r => _ => r)

      final def contrafmap[A](f: A => F[R]): Incomplete[A] =
        IncompleteContrafmap(this, f)

      final def contramap[A](f: A => R): Incomplete[A] =
        IncompleteContramap(this, f)

      protected[RouteBuilderModule] def respond_(r: Respond): Complete

      final def respond(r: R): Complete =
        respond_(respondUsing(r))
    }

    sealed trait Complete extends RouteBuilder {
      final override type Self = Complete

      final override protected[RouteBuilderModule] def whenW(w: When): Self =
        CompleteWhen(w, this)

      final override def |(orElse: RouteBuilder.Complete): Self =
        CompleteOrElse(this, orElse)

      final override def ensure(f: Req => Boolean, whenFalse: Respond): Self =
        CompleteEnsure(this, f, whenFalse)

      def build: Req ?=> F[RealRes]
    }

//    def lift(f: Req ?=> F[Response]): Complete =
//      Internals.Completed(f)

    def empty: Incomplete[Respond] =
      Empty

    // ===================================================================================================================

    private object Internals {

      case class When(cond: (Req => F[Boolean]) \/ (Req => Boolean)) {
        def apply[A](f: Req ?=> F[A]): Req ?=> F[A] =
          cond.fold(f.whenF(_), f.when)
      }

      object When {
        def pure(f: Req => Boolean): When = apply(\/-(f))
        def effect(f: Req => F[Boolean]): When = apply(-\/(f))
      }

      case object Empty extends Incomplete[Respond] {
        final override protected[RouteBuilderModule] val respondUsing = identity
        final override protected[RouteBuilderModule] def respond_(f: Respond) = f
      }

      case class IncompleteContrafmap[A, B](underlying: Incomplete[B], f: A => F[B]) extends Incomplete[A] {
        final override protected[RouteBuilderModule] val respondUsing = {
          val respondWithB = underlying.respondUsing
          a => Respond(req => f(a).flatMap(b => respondWithB(b).responder(req)))
        }

        final override protected[RouteBuilderModule] def respond_(r: Respond) =
          underlying.respond_(r)
      }

      case class IncompleteContramap[A, B](underlying: Incomplete[B], f: A => B) extends Incomplete[A] {
        final override protected[RouteBuilderModule] val respondUsing =
           underlying.respondUsing compose f

        final override protected[RouteBuilderModule] def respond_(r: Respond) =
          underlying.respond_(r)
      }

      case class IncompleteWhen[R](when: When, underlying: Incomplete[R]) extends Incomplete[R] {
        final override protected[RouteBuilderModule] def respondUsing =
          underlying.respondUsing

        final override protected[RouteBuilderModule] def respond_(r: Respond) =
          underlying.respond_(r).whenW(when)
      }

      case class IncompleteOrElse[R](self: Incomplete[R], orElse: Complete) extends Incomplete[R] {
        final override protected[RouteBuilderModule] def respondUsing =
          self.respondUsing

        final override protected[RouteBuilderModule] def respond_(r: Respond) =
          self.respond_(r) | orElse
      }

      case class CompleteWhen(when: When, underlying: Complete) extends Complete {
        final override def build =
          when(underlying.build)
      }

      case class CompleteOrElse(self: Complete, orElse: Complete) extends Complete {
        final override def build =
          self.build | orElse.build
      }

      case class CompleteEnsure(self: Complete, cond: Req => Boolean, whenFalse: Respond) extends Complete {

        final override def build =
          rewrite.build.when(cond) | whenFalse.build

        private def rewrite: Complete =
          self match {
            case _: Respond        => this
            case u: CompleteWhen   => u.underlying.ensure(cond, whenFalse).whenW(u.when)
            case u: CompleteOrElse => u.self.ensure(cond, whenFalse) | u.orElse.ensure(cond, whenFalse)
            case u: CompleteEnsure => copy(self = u.rewrite)
          }
      }

    }
  }
}