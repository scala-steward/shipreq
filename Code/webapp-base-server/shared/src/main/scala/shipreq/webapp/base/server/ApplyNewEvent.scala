package shipreq.webapp.base.server

import scalaz.{-\/, \/-}
import shipreq.webapp.base.data.Project
import shipreq.webapp.base.event._
import shipreq.webapp.base.hash.HashRec

object ApplyNewEvent {

  sealed abstract class Result
  case class  Updated(project: Project, ae: ActiveEvent, ve: VerifiedEvent) extends Result
  case class  Failed(reason: String)                                        extends Result
  case object NoChange                                                      extends Result

  def apply(e: ActiveEvent, p1: Project): Result =
    ApplyEvent.untrusted.apply1(e)(p1) match {
      case \/-(p2) =>
        val hrs = HashRec.changes(p1, p2)
        if (hrs.isEmpty)
          NoChange
        else {
          val ve = VerifiedEvent(e, hrs)
          Updated(p2, e, ve)
        }
      case -\/(err) => Failed(err)
    }

  def apply(r: MakeEvent.Result, p1: Project): Result =
    r match {
      case MakeEvent.MadeEvent(e) => apply(e, p1)
      case MakeEvent.NoChange     => NoChange
      case MakeEvent.Failed(e)    => Failed(e)
    }
}
