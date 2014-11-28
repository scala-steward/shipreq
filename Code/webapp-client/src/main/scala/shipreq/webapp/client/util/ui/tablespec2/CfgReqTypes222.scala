package shipreq.webapp.client.util.ui.tablespec2

//import japgolly.scalajs.react._
//import japgolly.scalajs.react.vdom.ReactVDom.all._
//import japgolly.scalajs.react.vdom.ReactVDom.{Tag => _, _}

import japgolly.scalajs.react.ScalazReact.ReactS
import shipreq.base.util.ScalaExt._
import shipreq.base.util.TaggedTypes.taggedStringInstance
import shipreq.webapp.base.UiText.FieldNames
import shipreq.webapp.base.data.DataImplicits._
import shipreq.webapp.base.data.ReqType.Mnemonic

import scalaz.effect.IO

//import shipreq.webapp.base.data.Validators.{reqType => V}
import shipreq.webapp.base.data._
import shipreq.webapp.base.data.delta.Partition
import shipreq.webapp.base.protocol.Routines.CustomReqTypeCrud
import shipreq.webapp.client.lib._
//import shipreq.webapp.client.util.ui.Util.checkbox
//import shipreq.webapp.client.util.ui.table._
//import shipreq.webapp.client.util.ui.{Editors => E}

import scalaz.std.anyVal.booleanInstance
import scalaz.std.string.stringInstance
import scalaz.std.tuple._

object __Validators {
  import shipreq.webapp.base.AppConsts._
  import shipreq.webapp.base.TextMod._
  import shipreq.webapp.base.UiText.FieldNames
  import shipreq.webapp.base.validation2._
  import Constraints._
  import GenericValidators._

  object reqType {

    type S = (Stream[CustomReqType], Option[CustomReqType.Id])

    val mnemonicU = {
      val validChars = WhitelistCharsR("A-Z", "may only consist of letters.")
      val validLength = LengthInRange(reqTypeMnemonicLength)
      Validator(
        CorrectionPart.endo(noWhitespace andThen upperCase)
          .addLiveCorrect(upperCase.run andThen validChars.live.run andThen validLength.live.run),
        ValidationPart.forConstraint(FieldNames.mnemonic, nonEmpty >> (validChars.constraint + validLength))
          .map(ReqType.Mnemonic)
        )
    }

    private def mnemonicUniqueness = {
      val static = (none[CustomReqType.Id],  ReqType.staticMnemonics)
      Uniqueness.againstSetByKeyO[S, CustomReqType.Id, Mnemonic](
        sr => sr._2,
        sr => static #:: sr._1.map(_.tmap2(_.id.some, _.allMnemonics))
      ).fieldName(FieldNames.mnemonic)
    }

    val mnemonicS = mnemonicU.liftS[S].addValidation(mnemonicUniqueness)

    val nameU = mandatoryShortText("Name")

    private def nameUniqueness =
      Uniqueness.entity[CustomReqType].applyO(_.id.some, _.name).fieldName("Name")

    val nameS = nameU.liftS[S].addValidation(nameUniqueness)
  }

  object customIncmpType {
    def key = refKey
    def desc = optionalLargeText(FieldNames.desc)
  }

  val refKey = {
    // DD-18: Hashtag-like refkeys (groupings, incmp) must match this format: /[A-Za-z0-9][A-Za-z0-9_-=.]*/
    // Must not contain: []{}<>
    // TODO should uniqueness and matching be case-insensitive?
    val validChars = WhitelistCharsR("""A-Za-z0-9\._=\-""", "may only consist of letters, numbers, and these symbols: . _ = -")
    val validLength = LengthInRange(refKeyLength)
    Validator(
      CorrectionPart.endo(noWhitespace)
        .addLiveCorrect(validChars.run andThen truncateToLength(refKeyLength).run),
      ValidationPart.forConstraint(FieldNames.refKey, nonEmpty >> (startsWithAlphaNumeric + validChars + validLength))
        .map(RefKey.apply)
      )
  }
}
import __Validators.{reqType => V}

import shipreq.base.util.ScalaExt._
import shipreq.webapp.base.validation2._
import scala.language.reflectiveCalls
import Editors.{EditorExtII, EditorExtV, EditorExt}
import monocle._
import monocle.syntax._

object CfgReqTypes222 {

  val tableIO = new TableIO[CustomReqTypeAndId, CustomReqTypeCrud, CustomReqTypeCrud.type]
  import tableIO.{D, P}

  val fields = FieldSet3[P](_.mnemonic.value, _.name, _.imp)(("", "", ImplicationNotRequired))

  val savedRowStore = SavedRowStore.of(fields).keyedBy[D]
  val newRowStore   = NewRowStore.of(fields)
  case class State(savedRows: savedRowStore.State, newRow: newRowStore.State)
  object State {
    private[this] def l = Lenser[State]
    val _savedRows = l(_.savedRows)
    val _newRow    = l(_.newRow)
  }
  type S = State
  type RowId = Option[D]
  val ST = ReactS.FixT[IO, S]

  val mnemonicE = Editors.textInputEditor.applyValidator(V.mnemonicS)
  val nameE     = Editors.textInputEditor.applyValidator(V.nameS)
  val impE      = Editors.checkboxEditor.imap(ImplicationRequired)

//  private val prespec = TableSpecBuilder[P](
//    FieldSpec[P](_.mnemonic.value)(V.mnemonic)(E.TextInputEditor),
//    FieldSpec[P](_.name)(V.name)(E.TextInputEditor),
//    FieldSpec[P].noValidation(_.imp, ImplicationRequired)(E.CheckboxEditor))
//    .dataId[D]
//
//  private val spec = prespec
//    .tableConstraints(
//      Some(mnemonicUniqueness),
//      Some(prespec.uniquenessCheck(_.name).fieldName("Name")),
//      None)
//    .saveNotNeededWhenE(p => (p.mnemonic, p.name, p.imp))
//    .asyncSaveP(tableIO.updateIO)
//
//  private val specC = TableSpecC(spec)(tableIO.createIO)
//
//  private val specD = TableSpecD(spec)(_.alive, tableIO.deleteIO)
//
//  private val compI = tableIO.innerComponent(spec, Partition.CustomReqTypes, renderInner)
//
//  val comp = tableIO.outerComponent("Cfg: Requirement Types", compI)


//  private def cells = new CfgTableCells[P, spec.VV, (Modifier, Set[ReqType.Mnemonic], Modifier, Modifier)] {
//    override def mklist = {
//      case (mnemonic, oldMnemonics, name, impReq) =>
//        val mn: Modifier =
//          if (oldMnemonics.isEmpty)
//            mnemonic
//          else
//            Seq(mnemonic, div(cls := "oldMnemonics", oldMnemonics.toStream.map(_.value).sorted.mkString(", ")))
//        List(mn, name, impReq)
//    }
//    override def newRow = {
//      case (mnemonic, name, impReq) => (mnemonic, Set.empty, name, impReq)
//    }
//    override def savedRow = {
//      case ((mnemonic, name, impReq), p) => (mnemonic, p.oldMnemonics, name, impReq)
//    }
//    override def deletedRow = p =>
//      (raw(p.mnemonic), p.oldMnemonics, raw(p.name), checkbox(ImplicationRequired from p.imp)(disabled := true))
//  }
//
//  private val tbl = CfgTable[CustomReqTypeAndId].b1(spec)(specC, specD, ("", "", false), _.mnemonic).b2(cells)
//
//  private def renderInner(S: ComponentScopeU[tableIO.Props, prespec.S, _]): ReactElement =
//    tbl(S.props.showDeleted, S)(S.props.x)
//      .tableness(List("Mnemonic", "Name", "Implication Required"), staticRows #::: _)
//
//  private val staticRows: tbl.RowStream = {
//    def rr(r: ReqType.Static) = {
//      val imp = checkbox(ImplicationRequired from r.imp)(disabled := true)
//      tbl.row("static", RowStatus.Sync, (raw(r.mnemonic), r.oldMnemonics, raw(r.name), imp), EmptyTag)(keyAttr := r.mnemonic.value)
//    }
//    ReqType.static.map(r => r.mnemonic -> rr(r)).toStream
//  }
}