package shipreq.webapp.member.project.library

import japgolly.scalajs.react.extra.Px
import japgolly.scalajs.react.{Callback, CallbackTo}
import shipreq.webapp.member.project.data.Project
import shipreq.webapp.member.project.event.VerifiedEvent
import shipreq.webapp.member.project.util.DataReusability.reusabilityProject

final class MutableProjectLibrary[PL <: ProjectLibrary](initialState: PL) {

  private var _state: PL =
    initialState

  val get: CallbackTo[PL] =
    CallbackTo(_state)

  private val _pxProject: Px.ThunkM[Project] =
    Px.apply(_state.latest).withReuse.manualRefresh

  val pxProject: Px[Project] =
    _pxProject

  def update(ves: VerifiedEvent.Seq): Callback =
    Callback.unless(ves.isEmpty) {
      get.flatMap { s1 =>
        Callback.traverseOption(s1.update(ves))(u => Callback {

          // Update state

          // if (s2.futureEvents.nonEmpty)
          //   console.warn(s"Not all events applied: stuck at #${s2.latestEventOrd.value} pending ${s2.futureEventRange}")
          _state = u.newLibrary.asInstanceOf[PL] // cbf jumping through hoops for type-level proof of this
          if (u.newlyAppliedEvents.nonEmpty)
            _pxProject.refresh()

        })
      }
    }
}

object MutableProjectLibrary {

  def apply[PL <: ProjectLibrary](initialState: PL): MutableProjectLibrary[PL] =
    new MutableProjectLibrary(initialState)

}
