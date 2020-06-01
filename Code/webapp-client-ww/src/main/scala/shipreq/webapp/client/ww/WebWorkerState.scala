package shipreq.webapp.client.ww

import japgolly.scalajs.react.Callback
import shipreq.webapp.base.event.{ProjectAndOrd, VerifiedEvent}

final class WebWorkerState {
  import WebWorkerState.Immutable

  private var s: Immutable =
    Immutable.init

  def setProject(po: ProjectAndOrd): Callback =
    Callback {
      println(s"setProject -- ${po.ord}")
      s = po
    }

  def updateProject(ves: VerifiedEvent.NonEmptySeq): Callback =
    Callback {
      assert(ves.min.ord.immediatelyFollowsLatest(s.ord), s"${ves.min.ord} doesn't follow ${s.ord}")
      s = s.mustApplyVerified(ves)
      println(s"updateProject -- ${ves.toList.map(_.ord)} ==> ${s.ord}")
    }
}

// =====================================================================================================================

object WebWorkerState {

  type Immutable = ProjectAndOrd

  object Immutable {
    def init: Immutable =
      ProjectAndOrd.empty
  }

}
