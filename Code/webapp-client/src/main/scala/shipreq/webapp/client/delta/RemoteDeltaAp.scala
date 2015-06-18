package shipreq.webapp.client.delta

import shipreq.base.util.UnivEq
import shipreq.webapp.base.data._
import shipreq.webapp.base.delta._
import shipreq.webapp.base.protocol._

object RemoteDeltaAp {

  sealed trait Result
  case class Success(p: Project, d: LocalDelta) extends Result
  case object Failure extends Result

  def apply(p: Project, rd: RemoteDelta): Result =
    rd.values.foldLeft[Result](Success(p, LocalDelta.empty))(apply2)

  private def apply2(acc: Result, rd: RemoteDeltaPR): Result = {

    def continue(s: Success): Result = {
      def go[P <: Partition](ppi: PPI[P])(d: RemoteDeltaP.Aux[P])(implicit ev: UnivEq[d.partition.Id]): Result =
        rd.applicability(ppi.rev(s.p)) match {
          case Applicable =>
            val newRev = rd.revRange.toInclusive.succ
            val newPrj = ppi.update(s.p, newRev, d)
            val localD = s.d + LocalDeltaP(d.partition)(d.delete, d.update)
            Success(newPrj, localD)

          case Inapplicable => Failure
          case Irrelevant   => s
        }

      rd.delta.fold(
        go(CustomIssueTypeProtocol.ppi),
        go(CustomReqTypeProtocol  .ppi),
        go(FieldProtocol          .PPI),
        go(TagProtocol            .PPI))
    }

    acc match {
      case a: Success => continue(a)
      case Failure    => Failure
    }
  }
}
