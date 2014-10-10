package shipreq.webapp.client

import org.scalajs.dom
import shipreq.webapp.client.delta._
import shipreq.webapp.shared.data.Project
import shipreq.webapp.shared.data.delta.{Rev, RemoteDelta}
import japgolly.scalajs.react.experiment.Broadcaster
import scalaz.effect.IO

final class ClientData(initial: Project) extends Broadcaster[LocalDelta] {

  private var p = initial

  @inline def project = p

  def update(d: RemoteDelta): IO[Unit] =
    RemoteDelta(p, d) match {
      case Applied(p2, d2) =>
        IO{ p = p2; broadcast(d2) }
      case CouldntApply =>
        Console.errorIO(s"Update failed.\n\nΠ: $p\n\nΔ: $d")
    }
}

object ClientData {

  // TODO Restrict access to global data so that components don't have direct access. private[...]
  val GLOBAL = { // TODO Global client state given fake bullshit data
    import shipreq.webapp.shared.data._
    import CustomReqType.Id
    implicit def autoMnemonic(s: String) = ReqType.Mnemonic(s)
    val list = List(
      CustomReqType(Id(1), "CO", Set.empty, "Constraint", ImplicationNotRequired, Alive),
      CustomReqType(Id(2), "MF", Set.empty, "Major Feature", ImplicationNotRequired, Alive),
      CustomReqType(Id(3), "FR", Set.empty, "Functional Requirement", ImplicationRequired, Alive),
      CustomReqType(Id(4), "BR", Set.empty, "Business Rule", ImplicationNotRequired, Alive),
      CustomReqType(Id(5), "DD", Set("DA", "DDF"), "Data Definition", ImplicationNotRequired, Dead),
      CustomReqType(Id(6), "SI", Set.empty, "Solution Idea", ImplicationRequired, Dead)
    )
    //val map = list.map(i => i.id -> i).toMap
    val rev = Rev(6)
    new ClientData(Project(CustomReqTypes(rev, list)))
  }
}