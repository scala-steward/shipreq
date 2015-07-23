package shipreq.webapp.client.app.state

import japgolly.scalajs.react.extra.Broadcaster
import scalaz.effect.IO
import scalaz.{-\/, \/-}
import shipreq.webapp.base.data.Project
import shipreq.webapp.base.event.{ApplyEvent, VerifiedEvents}
import shipreq.webapp.base.protocol.ProjectInit
import shipreq.webapp.client.lib.{ConsoleIO, SuccessIO}
import shipreq.webapp.client.protocol.ClientProtocol

final class ClientData(init: Project) extends Broadcaster[Changes] {

  private[this] var _p = init

  @inline def project = _p

  def applyEvents(ves: VerifiedEvents): IO[Unit] = IO {
    val p1 = _p
    ApplyEvent.trusted.applyVerified(ves)(p1) match {
      case \/-(p2) =>
        _p = p2
        broadcast(Changes(ves, p1, p2))
      case -\/(err) =>
        // TODO Do more when VerifiedEvent application fails
        ConsoleIO(_ error s"Update failed. $err").unsafePerformIO()
    }
  }

  def applyEventsS(ves: VerifiedEvents): SuccessIO =
    SuccessIO(applyEvents(ves))
}

object ClientData {

  def init(cp: ClientProtocol, remoteInit: ProjectInit.Instance, onSuccess: ClientData => IO[Unit]): IO[Unit] =
    cp.call(remoteInit)((),
      p => SuccessIO(onSuccess(new ClientData(p))),
      cp.consumeGenericFailure) // TODO handle failure properly
}