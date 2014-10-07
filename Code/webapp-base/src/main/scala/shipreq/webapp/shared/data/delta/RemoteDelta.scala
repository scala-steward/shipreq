package shipreq.webapp.shared.data.delta

import shipreq.webapp.shared.data.Project
import Partition._

final case class RemoteDeltaP[P <: Partition](del: List[P#Id],
                                              upd: List[P#Instance]) {

  def isEmpty = del.isEmpty || upd.isEmpty
  def nonEmpty = !isEmpty

  private type _P = P

  def updateSet = new LocalDeltaP[P](del, upd)

  def updateSetG(_p: P): LocalDeltaG = new LocalDeltaG {
    override type P = _P
    override def p = _p
    override def deltaP = RemoteDeltaP.this.updateSet
  }
}

final case class RemoteDeltaG(meta: Partition,
                              updateSet: RemoteDeltaP[Partition],
                              fromRev: Rev,
                              toRev: Rev) {

  def applicableToRev(tgt: Rev): Applicability =
    if (tgt.value < fromRev.value - 1)
      Unapplicable
    else if (tgt.value >= toRev.value)
      NoNeed
    else
      Applies

  def isEmpty = updateSet.isEmpty
  def nonEmpty = !isEmpty
}

sealed trait ApplicationResult
sealed trait Applicability
case object Applies extends Applicability
case object NoNeed extends Applicability
case object Unapplicable extends ApplicationResult with Applicability
case class Applied(p: Project, u: LocalDeltas) extends ApplicationResult

final class RemoteDelta(deltas: RemoteDeltas) {
  
  def apply(p: Project, deltas: RemoteDeltas): ApplicationResult =
    apply2(Applied(p, Nil), deltas)

  private def apply2(z: ApplicationResult, deltas: RemoteDeltas): ApplicationResult =
    deltas.foldLeft(z)((acc, d) => {

      def y(a: Applied): ApplicationResult = {
        def x[P <: Partition](m: P, b: Fns[P]): ApplicationResult =
          d.applicableToRev(b.rev(a.p)) match {
            case Applies =>
              val ds = forceEqProof[Partition, P].subst(d.updateSet)
              val p = b.update(a.p, d.toRev, ds)
              val u = ds.updateSetG(m) :: a.u
              Applied(p, u)
            case NoNeed => a
            case Unapplicable => Unapplicable
          }

        d.meta match {
          case t@ CustReqType => x(t, CustReqTypeFns)
        }
      }
      
      acc match {
        case a@ Applied(_, _) => y(a)
        case Unapplicable => Unapplicable
      }
    })
}
