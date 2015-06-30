package shipreq.webapp.base.delta

import monocle.Lens
import shipreq.base.util.IMap
import shipreq.webapp.base.data._

/**
 * PPI = Partition Project Interface.
 *
 * Defines how a partition applies to a project.
 * For each partition, there must be an instance of this class available.
 */
abstract class PPI[P <: Partition] {
  def rev(p: Project): Rev
  def update(p: Project, newRev: Rev, d: RemoteDeltaP.Aux[P]): Project
}

object PPI {

  def lens[D](p: Partition, lens: Lens[Project, RevAnd[D]])
                    (updateFn: (RemoteDeltaP.Aux[p.type], D) => D): PPI[p.type] =
    new PPI[p.type] {

      def rev(project: Project): Rev =
        lens.get(project).rev

      def update(project: Project, newRev: Rev, delta: RemoteDeltaP.Aux[p.type]): Project = {
        val oldData  = lens.get(project).data
        val newData  = updateFn(delta, oldData)
        val newValue = RevAnd(newRev, newData)
        lens.set(newValue)(project)
      }
    }

  def imap(p: Partition)(l: Lens[Project, RevAnd[IMap[p.Id, p.Data]]]): PPI[p.type] =
    lens(p, l) { (delta, d0) =>

      // Deletions
      val d1 = d0 -- delta.delete

      // Updates
      d1.addAll(delta.update: _*)
    }
}
