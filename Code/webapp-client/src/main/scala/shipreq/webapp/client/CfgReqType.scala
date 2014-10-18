package shipreq.webapp.client

import scalaz.std.anyVal.booleanInstance
import scalaz.std.string.stringInstance
import scalaz.std.tuple._
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.experiment.OnUnmount

import shipreq.base.util.TaggedTypes.taggedStringInstance
import shipreq.webapp.base.data._
import shipreq.webapp.base.data.delta.Partition
import shipreq.webapp.base.protocol.{DeletionAction, Routines}
import shipreq.webapp.client.lib.TableIO
import shipreq.webapp.client.util.ui.table._
import shipreq.webapp.client.util.ui.{Editors => E, Util}
import Validators.{reqType => V}
import ReqType.Mnemonic
import DeletionAction._
import Routines.CustomReqTypeCrud
import DataImplicits._

object CfgReqType {

  val tableIO = new TableIO[CustomReqTypeAndId, CustomReqTypeCrud, CustomReqTypeCrud.type]
  import tableIO.{P, D, Arb}

  private val prespec = TableSpecBuilder[P](
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
            .map(_._2.p)
        val static: Stream[ReqType] = ReqType.static.toStream
        (static #::: custom).flatMap(p => p.mnemonic #:: p.oldMnemonics.toStream)
      }).fieldName("Mnemonic")

  private val spec = prespec
    .tableConstraints(
      Some(mnemonicUniqueness),
      Some(prespec.uniquenessCheck(_.name).fieldName("Name")),
      None)
    .saveNotNeededWhenE(p => (p.mnemonic, p.name, p.imp))
    .asyncSaveP(_.id, tableIO.saveIO)

  private val deletion =
    new AsyncDeletion(spec)(_.alive, tableIO.deleteIO)

  private val newRowS =
    spec.unsavedInitS(("","",false))

  // ===================================================================================================================
  // Component

  case class Props(x: Arb, showDeleted: Boolean)

  private final class Backend extends OnUnmount

  val Component = ReactComponentB[Props]("CfgReqTypes")
    .getInitialState(p => p.showDeleted)
    .render(Render.renderOuter _)
    .build

  private val InnerComponent = ReactComponentB[Props]("CfgReqTypesⁱ")
    .getInitialState(p => spec.initialState(p.x._2.project.customReqTypes.data, _.id))
    .backend(_ => new Backend)
    .render(Render.renderInner _)
    .configure(tableIO.recvExtUpdates(spec, Partition.CustomReqTypes, _.x))
    .build

  // ===================================================================================================================
  private object Render {
    import japgolly.scalajs.react._, vdom.ReactVDom.{Tag => _, _}, all._, ScalazReact._
    import Util.checkbox

    def renderOuter(S: ComponentScopeU[Props, Boolean, Unit]): VDom = {
      val s = S.state
      div(
        label(
          checkbox(s)(onchange --> S.modState(b => !b)),
          raw(if (s) "Showing deleted" else "Not showing deleted")),
        InnerComponent(S.props.copy(showDeleted = s)))
    }

    type ScopeI = ComponentScopeU[Props, prespec.S, Backend]
    type RowStream = Stream[(Mnemonic, Tag)]

    def renderInner(S: ScopeI): VDom = {
      implicit val x: Arb = S.props.x
      val nonNewRows = (staticRows #::: savedRows(S) #::: deletedRows(S)).sortBy(_._1.value).map(_._2).toJsArray
      div(
        button(onclick ~~> S.runState(newRowS), disabled := spec.unsavedRowExists(S), "New"),
        table(
          thead(tr(th("Mnemonic"), th("Name"), th("Implication Required"), th("Ctrls"))),
          tbody(newRow(S), nonNewRows)))
    }

    private def row(classArg: String, mnemonic: Modifier, oldMnemonics: Set[ReqType.Mnemonic], name: Modifier, impReq: Modifier, rs: RowStatus, ctrls: => Modifier): Tag = {
      val (cls2, c: Modifier) = rs match {
        case RowStatus.Sync          => ("sync", ctrls)
        case RowStatus.Locked        => ("locked", img(cls := "spinner", src := "/assets/loading-spin.svg"))
        case RowStatus.Failed(retry) => ("failed", button("Retry", onclick ~~> retry))
      }
      val mn: Modifier =
        if (oldMnemonics.isEmpty)
          mnemonic
        else
          Seq(mnemonic, div(cls := "oldMnemonics", oldMnemonics.toStream.map(_.value).sorted.mkString(", ")))
      tr(cls := s"$classArg $cls2", td(mn), td(name), td(impReq), td(c))
    }

    def newRow(S: ScopeI)(implicit x: Arb) =
      spec.unsavedRow((F, rs, vv) => {
        val (mnemonic, name, impReq) = vv
        def c = button(onclick ~~> F.runState(spec.unsavedRemoveS), "Cancel")
        row("new", mnemonic, Set.empty, name, impReq, rs, c)(keyAttr := "new")
      })(x)(S)

    def savedRows(S: ScopeI)(implicit x: Arb): RowStream = {
      val rr = spec.savedRowP((F, id, rs, p, vv) => {
        val (mnemonic, name, impReq) = vv
        def c = deletion.buttons(F, id, HardDel, SoftDel)
        row("live", mnemonic, p.oldMnemonics, name, impReq, rs, c)(keyAttr := id.value)
      })(x)(S)
      deletion.savedGetP(S, Alive).map(p => p.mnemonic -> rr(p.id))
    }

    def deletedRows(S: ScopeI)(implicit x: Arb): RowStream = {
      def rr(rs: RowStatus, p: P) = {
        val impReq = checkbox(ImplicationRequired from p.imp)(disabled := true)
        def c = deletion.button(S, p.id, Restore)
        row("dead", raw(p.mnemonic), p.oldMnemonics, raw(p.name), impReq, rs, c)(keyAttr := p.id.value)
      }
      if (S.props.showDeleted)
        deletion.savedGet(S, Dead).map(r => r.p.mnemonic -> rr(r.status, r.p))
      else
        Stream.empty
    }

    val staticRows: RowStream = {
      def rr(r: ReqType.Static) = {
        val imp = checkbox(ImplicationRequired from r.imp)(disabled := true)
        row("static", raw(r.mnemonic), r.oldMnemonics, raw(r.name), imp, RowStatus.Sync, EmptyTag)(keyAttr := r.mnemonic.value)
      }
      ReqType.static.map(r => r.mnemonic -> rr(r)).toStream
    }
  }
}