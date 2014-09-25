package shipreq.webapp.client

import monocle.{Iso, SimpleLens}
import org.scalajs.dom.console
import shipreq.base.util.TaggedTypes.taggedStringInstance

import scalaz.effect.IO
import scalaz.std.anyVal.booleanInstance
import scalaz.std.string.stringInstance
import shipreq.webapp.shared.data._
import shipreq.webapp.client.ui.table._
import shipreq.webapp.client.ui.{Editors => E, Util}
import Validators.{reqType => V}
import ReqType.Mnemonic

object CfgReqType {

  type P = CustReqType
  type D = CustReqType.Id

  val prespec = TableSpecBuilder[P](
    FieldSpec[P](_.mnemonic.value)(V.mnemonic)(E.TextInputEditor),
    FieldSpec[P](_.name)(V.name)(E.TextInputEditor),
    FieldSpec[P].noValidation(_.imp, ImplicationRequired)(E.CheckboxEditor))
    .dataId[D]

  private def mnemonicUniqueness =
    TableConstraint.uniquenessE[prespec.S, prespec.R, Mnemonic](
      (s, r) => {
        val custom: Stream[ReqType] =
          s._1.toStream
            .filterNot(dpi => r.fold(false)(_ == dpi._1)) // exclude own row
            .map(_._2._1)
        val static: Stream[ReqType] = ReqType.static.toStream
        (static #::: custom).flatMap(p => p.mnemonic #:: p.oldMnemonics.toStream)
      }).fieldName("Mnemonic")

  val spec = prespec
    .tableConstraints(
      Some(mnemonicUniqueness),
      Some(prespec.uniquenessCheck(_.name).fieldName("Name")),
      None)
    .saveFn2(fakeSave, _.id)

  // AJAX is async
  def fakeSave(op: Option[P], newValues: prespec.U) = IO[P] {
    val (a,b,c) = newValues
    op match {
      case None =>
        console.log(s"FAKE-SAVE: New row $newValues")
        CustReqType(CustReqType.Id(666L), a, Set.empty, b, c, Alive)
      //        case Some(old) if old.value == newValues =>
      //          old
      case Some(p) =>
        console.log(s"FAKE-SAVE: Update [$p] → $newValues")
        p
    }
  }

  val deletion = new DeletionManager(spec)(
    SimpleLens[P](_.alive)((a,b) => a.copy(alive = b)),
    id => a => IO(a match {
//      case HardDelete => FakeDao.customReqType.deleteHard(id)
//      case SoftDelete => FakeDao.customReqType.deleteSoft(id)
//      case Restore    => FakeDao.customReqType.restore(id)
      case x => console.log(s"FAKE DELETE: $x on $id")
    }))

  object Action {
    val newRow =
      spec.createUnsaved(("","",false))
  }

  // ------------------

  type Props = ReqTypeTableProps

  case class ReqTypeTableProps(qqq: ProjectReqTypes, showDeleted: Boolean)

  import japgolly.scalajs.react.ReactComponentB

  val ReqTypeTableComp = ReactComponentB[ReqTypeTableProps]("CfgReqTypesⁱ")
    .getInitialState(p => spec.initialState(p.qqq.customReqTypes))
    .render(Render.renderInner _)
    .create

  val ReqTypeTableCompOuter = ReactComponentB[ReqTypeTableProps]("CfgReqTypes")
    .getInitialState(p => p.showDeleted)
    .render(Render.renderOuter _)
    .create

  // ------------------

  object Render {
    import japgolly.scalajs.react._
    import japgolly.scalajs.react.vdom.ReactVDom._
    import japgolly.scalajs.react.vdom.ReactVDom.all._
    import japgolly.scalajs.react.ScalazReact._

    private def row(mnemonic: Modifier, name: Modifier, impReq: Modifier, delButton: Modifier) =
      Seq(td(mnemonic), td(name), td(impReq), td(delButton))

    val newRow =
      spec.unsavedRow((T, vv) => {
        val (mnemonic, name, impReq) = vv
        val delButton = button(onclick ~~> T.runState(spec.removeUnsavedS))("Cancel")
        tr(keyAttr := "new", row(mnemonic, name, impReq, delButton))
      })

    val savedRow =
      spec.savedRow((T, id, p, vv) => {
        val (mnemonic, name, impReq) = vv
        tr(keyAttr := id.value, row(mnemonic, name, impReq, deletion.buttons(T, id, HardDelete, SoftDelete)))
      })

    def deletedRow(T: ComponentStateFocus[prespec.S], p: P) =
      tr(cls := "del", key := p.id.value, row(
        raw(p.mnemonic),
        raw(p.name),
        Util.checkbox(ImplicationRequired from p.imp)(disabled := true),
        deletion.button(T, p.id, Restore)))

    def staticRow(r: ReqType.Static) =
      tr(key := r.mnemonic.value, row(
        raw(r.mnemonic),
        raw(r.name),
        Util.checkbox(ImplicationRequired from r.imp)(disabled := true),
        Nop))

    def renderInner(T: ComponentScopeU[Props, prespec.S, Unit]): VDom = {
      val newRow = Render.newRow.render(T)(())

      type RS = Stream[(Mnemonic, Tag)]

      def savedRows: RS = {
        val rr = Render.savedRow.render(T)
        deletion.getSavedP(T, Alive).map(p => p.mnemonic ->  rr(p.id))
      }

      def deletedRows: RS =
        if (T.props.showDeleted)
          deletion.getSavedP(T, Dead).map(p => p.mnemonic -> Render.deletedRow(T, p))
        else
          Stream.empty

      def staticRows: RS = // TODO make val and list
        ReqType.static.toStream.map(r => r.mnemonic -> staticRow(r))

      val nonNewRows = (staticRows #::: savedRows #::: deletedRows).sortBy(_._1.value).map(_._2).toJsArray

      div(
        button(onclick ~~> T.runState(Action.newRow))("New"),
        table(
          thead(tr(th("Mnemonic"), th("Name"), th("Implication Required"), th("Ctrls"))),
          tbody(newRow, nonNewRows)
        )
      )
    }

    def renderOuter(t: ComponentScopeU[Props, Boolean, Unit]): VDom = {
      val s = t.state
      div(
        label(
          Util.checkbox(s)(onchange --> t.modState(b => !b)), // TODO --> instead of ~~>
          raw(if (s) "Showing deleted" else "Not showing deleted")
        ),
        ReqTypeTableComp(t.props.copy(showDeleted = s))
      )
    }
  }
}