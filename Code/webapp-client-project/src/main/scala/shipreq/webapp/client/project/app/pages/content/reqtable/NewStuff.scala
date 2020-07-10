package shipreq.webapp.client.project.app.pages.content.reqtable

import japgolly.microlibs.nonempty.NonEmptyVector
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import shipreq.base.util._
import shipreq.webapp.base.data.{CustomReqTypeId, ExternalPubid, ReqTypes, StaticReqType}
import shipreq.webapp.base.ui.Toast
import shipreq.webapp.client.project.app.pages.content.reqtable.NewStuff.State
import shipreq.webapp.client.project.feature.CreateFeature
import shipreq.webapp.client.project.feature.CreateFeature.RowKey
import shipreq.webapp.client.project.feature.SavedViewFeature.ColumnPlus
import shipreq.webapp.client.project.widgets.ProjectWidgets

/**
  * Unified, convenience interface to both [[NewButton]] and [[NewForm]].
  */
object NewStuff {

  sealed abstract class State {
    def close: State
    def toggle(r: RowKey.AndId): State
    def setSelection(r: RowKey.AndId): State
  }

  object State {
    final case class Open(selected: RowKey.AndId) extends State {
      override def close =
        Closed(Some(selected))

      override def setSelection(r: RowKey.AndId) =
        Open(r)

      override def toggle(r: RowKey.AndId) =
        Closed(Some(r))
    }

    final case class Closed(selected: Option[RowKey.AndId]) extends State {
      override def close =
        this

      override def setSelection(r: RowKey.AndId) =
        Closed(Some(r))

      override def toggle(r: RowKey.AndId) =
        Open(r)
    }

    def init: State =
      State.Closed(None)
  }

}

final class NewStuff(state        : State,
                     modState     : ModFn[State],
                     routerCtl    : RouterCtl[ExternalPubid],
                     pw           : ProjectWidgets.NoCtx,
                     toast        : Toast,
                     reqTypes     : ReqTypes,
                     allowRCG     : Permission,
                     create       : CreateFeature.ReadWrite.ForProject,
                     activeColumns: NonEmptyVector[ColumnPlus]) {

  private val buttonUpdate: Reusable[NewButton.Update] =
    modState.map(f =>
      NewButton.Update(
        select = s => f.modState(_.setSelection(s)),
        click  = s => f.modState(_.toggle(s))))

  val buttonProps: NewButton.Props =
    state match {
      case State.Open(s) =>
        var b = NewButton.Props(Some(s), reqTypes, allowRCG, pw, Some(buttonUpdate))
        // If what we thought was open is no longer acceptable, proceed as if closed
        if (b.dropdownProps.selected.forall(_ !=* s))
          b = b.copy(state = None)
        b

      case State.Closed(o) =>
        NewButton.Props(o, reqTypes, allowRCG, pw, Some(buttonUpdate))
    }

  private val cancel: Callback =
    modState.modState(_.close)

  private def codeGroupForm(r: RowKey.CodeGroup.AndId): Option[VdomElement] =
    Some(NewForm.ForCodeGroup.Props((), activeColumns, create(r), routerCtl, toast, cancel).render)

  private def reqForm(r: RowKey.Req.AndId): Option[VdomElement] =
    r.id match {
      case reqTypeId: CustomReqTypeId =>
        reqTypes.custom.get(reqTypeId).map { rt =>
          NewForm.ForGenericReq.Props(rt, activeColumns, create(r), routerCtl, toast, cancel).render
        }
      case StaticReqType.UseCase =>
        Some(NewForm.ForUseCase.Props((), activeColumns, create(r), routerCtl, toast, cancel).render)
    }

  val form: Option[VdomElement] =
    state match {
      case State.Open(s) if buttonProps.state.isDefined =>
        s.fold(
          codeGroup   = codeGroupForm,
          req         = reqForm,
          manualIssue = _ => None,
        )

      case _ =>
        None
    }

}
