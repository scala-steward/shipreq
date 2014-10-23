package shipreq.webapp.client.util.route

import japgolly.scalajs.react._
import japgolly.scalajs.react.ScalazReact._
import japgolly.scalajs.react.experiment._
import org.scalajs.dom._
import scala.scalajs.js
import scalaz.{\/, ~>, Free}
import scalaz.effect.IO
import scalaz.syntax.std.option._
import shipreq.webapp.client.util.OnUnmountBackend

final case class BaseUrl(value: String)

object Router {

  // TODO Listenable.installF monkey patch
  implicit class ListenableObjExt(val a: Listenable.type) extends AnyVal {
    def installF[P, S, B <: OnUnmount, M[_], A](f: P => Listenable[A], g: A => ReactST[M, S, Unit])(implicit M: M ~> IO, F: ChangeFilter[S]) =
      Listenable.install[P, S, B, A](f, t => a => t.runStateF(g(a)).unsafePerformIO())
  }

  @inline def component[P](base: BaseUrl, p: Page[P]) =
    component2(base, p.root, p.paths)

  def component2[P](base: BaseUrl, root: Root[P], paths: List[Path[P]]) = {
    val router  = new Router[P](base, root, paths)
    val rootDom = root renderer router
    val doms    = paths.map(p => (p: Route[P], p renderer router)).toMap + (root -> rootDom)

    ReactComponentB[Unit](base.value)
      .initialState(router.readIO.unsafePerformIO())
      .backend(_ => new OnUnmountBackend)
      .render((_, route, _) => doms.getOrElse(route, rootDom))
      .componentWillMount(_ => router.init.unsafePerformIO())
      .configure(Listenable.installF(_ => router, (_: Unit) => router.readS))
      .buildU
  }
}

class Router[P](base: BaseUrl, root: Root[P], paths: Seq[Path[P]]) extends Broadcaster[Unit] {

  final type RouteCmdP[A] = RouteCmd[P, A]
  final type RouteProgP[A] = RouteProg[P, A]

  @inline final private def regexEscape(s: String) =
    s.replaceAll("""([-()\[\]{}+?*.$\^|,:#<!\\])""", """\\$1""").replaceAll("""\u0008""", """\\u0008""")

  private val urlParser =
    s"""^(?://|[^/]+?)+?${regexEscape(base.value)}(.*)$$""".r

  val routes: Seq[Route[P]] =
    root +: paths

  def route(url: String): Option[Route[P]] =
    url match {
      case urlParser(path) => routes.find(_.path == path)
      case _               => None
    }

  def read(url: String): RouteCmdP[Route[P]] \/ Route[P] =
    route(url) toRightDisjunction ReplaceState(base, root)

  def readIO: IO[Route[P]] =
    IO(window.location.href)
      .flatMap(url => read(url).fold(interpret(_), IO(_)))

  def readS =
    ReactS.modT[IO, Route[P]](_ => readIO) // TODO use setM later

  def set(r: Route[P]): RouteProgP[Unit] =
    PushState(base, r) >> Notify

  def setIO(r: Route[P]): IO[Unit] =
    interpret(set(r))

  @inline final private def eo = js.Dynamic.literal()

  private[this] val cmdinterp: RouteCmdP ~> IO = new (RouteCmdP ~> IO) {
    @inline private def hs = js.Dynamic.literal()
    @inline private def ht = ""
    @inline private def mkurl(r: Route[P]) = base.value + r.path

    override def apply[A](m: RouteCmdP[A]): IO[A] = m match {
      case PushState(b, r)    => IO{ window.history.pushState   (hs, ht, mkurl(r)); r }
      case ReplaceState(b, r) => IO{ window.history.replaceState(hs, ht, mkurl(r)); r }
      case Notify             => IO{ broadcast(()) }
    }
  }

  def interpret[A](r: RouteProgP[A]): IO[A] =
    Free.runFC[RouteCmdP, IO, A](r)(cmdinterp)

  val init: IO[Unit] = {
    var need = true
    IO(
      if (need) {
        window.onpopstate = (_: PopStateEvent) => broadcast(())
        need = false
      }
    )
  }
}

sealed trait RouteCmd[+P, A]
case class PushState[P](b: BaseUrl, r: Route[P]) extends RouteCmd[P, Route[P]]
case class ReplaceState[P](b: BaseUrl, r: Route[P]) extends RouteCmd[P, Route[P]]
case object Notify extends RouteCmd[Nothing, Unit]
