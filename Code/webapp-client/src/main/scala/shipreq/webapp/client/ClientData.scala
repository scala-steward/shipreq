package shipreq.webapp.client

import japgolly.scalajs.react.extra.Broadcaster
import scalaz.{-\/, \/-}
import scalaz.effect.IO
import shipreq.webapp.client.delta._
import shipreq.webapp.client.protocol.ClientProtocol
import shipreq.webapp.base.data.Project
import shipreq.webapp.base.delta.RemoteDelta
import shipreq.webapp.base.event.{ApplyEvent, VerifiedEvents}
import shipreq.webapp.base.protocol.RemoteFns.ProjectInit
import shipreq.webapp.client.lib.{SuccessIO, FailureIO, ConsoleIO}

final class ClientData(init: Project) extends Broadcaster[LocalDelta] {

  private[this] var pvar = init

  @inline def project = pvar

  def applyEvents(ves: VerifiedEvents): IO[Unit] = IO {
    ApplyEvent.trusted.applyVerified(ves)(project) match {
      case \/-(p2) =>
        pvar = p2
        // TODO broadcast(localDelta)
      case -\/(err) =>
        // TODO Do more when VerifiedEvent application fails
        ConsoleIO(_ error s"Update failed. $err").unsafePerformIO()
    }
  }

  def applyEventsS(ves: VerifiedEvents): SuccessIO =
    SuccessIO(applyEvents(ves))

  def applyRemoteDelta(rd: RemoteDelta): IO[Unit] =
    RemoteDeltaAp(project, rd) match {
      case RemoteDeltaAp.Success(newProject, localDelta) => IO[Unit] {
        pvar = newProject
        broadcast(localDelta)
      }
      case RemoteDeltaAp.Failure =>
        ConsoleIO(_ error s"Update failed.\n\nΠ: $project\n\nΔ: $rd")
    }
}

object ClientData {

  def init(cp: ClientProtocol, remoteInit: ProjectInit.Instance, onSuccess: ClientData => IO[Unit]): IO[Unit] =
    cp.call(remoteInit)((),
      p => SuccessIO(onSuccess(new ClientData(p))),
      cp.consumeGenericFailure) // TODO handle failure properly
}