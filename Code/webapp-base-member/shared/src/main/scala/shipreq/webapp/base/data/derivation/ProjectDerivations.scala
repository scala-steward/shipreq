package shipreq.webapp.base.data.derivation

import shipreq.base.util.storecache._
import shipreq.webapp.base.data._
import ProjectDerivations.Logic

final case class ProjectDerivations()

object ProjectDerivations {

  def next(prev: Option[ProjectDerivations], project: Project): ProjectDerivations = {
    apply()
  }

  // ===================================================================================================================

  object QuickEqInstances {
  }

  // ===================================================================================================================

  object Logic {
    import QuickEqInstances._

  }
}
