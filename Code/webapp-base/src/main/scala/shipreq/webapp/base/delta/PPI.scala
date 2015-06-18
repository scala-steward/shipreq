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
  def imap(p: Partition)(lens: Lens[Project, RevAnd[IMap[p.Id, p.Data]]]): PPI[p.type] =
    new PPI[p.type] {

      def rev(project: Project): Rev =
        lens.get(project).rev

      def update(project: Project, newRev: Rev, delta: RemoteDeltaP.Aux[p.type]): Project = {
        var m = lens.get(project).data

        // Deletions
        m --= delta.delete

        // Updates
        m = m.addAll(delta.update: _*)

        lens.set(RevAnd(newRev, m))(project)
      }
    }
}
