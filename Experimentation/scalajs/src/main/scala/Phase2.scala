import monocle._
import monocle.syntax._
import monocle.function.Field1._
import monocle.function.Field2._
import org.scalajs.dom
import org.scalajs.dom.console
import scala.scalajs.js
import scalaz.{State, StateT, Scalaz, Bind}
import scalaz.syntax.bind._
import scalaz.std.option.optionInstance
import Scalaz.Id
import scalaz.effect.IO
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.ReactVDom._
import japgolly.scalajs.react.vdom.ReactVDom.all._
import japgolly.scalajs.react.ScalazReact._
import FormStuff._
import Lib._

object Phase2 extends js.JSApp {
  override def main(): Unit = {
    import Phase2.IssueConfig._

    IssueTypeTable(List(
      1L -> CustomIssueType("TODO", None)
      ,2L -> CustomIssueType("TBD", Some("To Be Decided."))
    )) render dom.document.getElementById("target")

    DragAndDrop.Component(List(
      DragAndDrop.Item(10, "Ten")
      ,DragAndDrop.Item(20, "Two Zero")
      ,DragAndDrop.Item(30, "Firty")
      ,DragAndDrop.Item(40, "Thorty")
      ,DragAndDrop.Item(50, "Fipty")
    )) render dom.document.getElementById("target2")
  }

  // ===================================================================================================================

  object IssueConfig {

    type CustomIssueTypeId = Long
    case class CustomIssueType(key: String, desc: Option[String])

    type P = CustomIssueType
    val PreSpec = SpecBuilder[P](
                    SpecAttr[P](_.key)(KeyValidator)(TextInputEditor),
                    SpecAttr[P](_.desc)(DescValidator)(TextareaEditor)
                  ).buildO(CustomIssueType.apply)
                  .rowId[CustomIssueTypeId]
    val Spec = PreSpec.ctxAwareValidators(Some(PreSpec.uniquenessCheck(_.key)), None)
                 .saveFn(fakeSave)

    type Px = (CustomIssueTypeId, P)

    def fakeSave(p: Option[Px], g: CustomIssueType) = IO[Px] {
      console.log(s"SAVING $p ⇒ $g")
      val newId = p.fold[CustomIssueTypeId](666L)(_._1)
      (newId, g)
    }

    def fakeDelete(id: CustomIssueTypeId) = IO {
      console.log(s"DELETING $id")
    }

    object NewRow {
      val create = Spec.createUnsaved(("",""))
      val row = Spec.unsavedRow((T, vv) => {
        val (key, desc) = vv
        val delButton = button(onclick ~~> T.modStateIO(Spec.removeUnsaved))("Cancel")
        tr(keyAttr := "new")(td(key), td(desc), td(delButton))
      })
    }

    object SavedRow {
      private val delete = Spec.deleteSavedFn(fakeDelete)
      val row = Spec.savedRow((T, id, vv) => {
        val (key, desc) = vv
        val delButton = button(onclick ~~> T.runStateIO(delete(id)))("Delete")
        tr(keyAttr := id)(td(key), td(desc), td(delButton))
      })
    }

    val IssueTypeTable = ReactComponentB[List[(CustomIssueTypeId, CustomIssueType)]]("IssueTypeTable")
      .getInitialState(p => Spec.initialState(p))
      .render(T => {
        val S = T.state
        //console.log(s"State = $S")

        val newRow = NewRow.row.render(T)(())
        val savedRows = Spec.renderSaved(T, SavedRow.row)(_.sortBy(_._2._1.key))

        // TODO handle empty table
        div(
          button(onclick ~~> T.runStateIO(NewRow.create))("Create"),
          table(
            thead(tr(th("Name"), th("Description"), th("Ctrls"))),
            tbody(newRow, savedRows)
          )
        )
      }).create
    }

  // ===================================================================================================================

  object DragAndDrop {

    case class Item(id: Int, name: String)

    val RowComp = DND.Child.dndItemComponent[Item](
      (i, hnd) => hnd :: raw(s"${i.id} | ${i.name}") :: Nil)

    case class ParentState(items: List[Item], dnd: DND.Parent.PState[Item])

    def itemCmp(a: Item, b: Item) = a.id==b.id

    val Component = ReactComponentB[List[Item]]("DragAndDrop")
      .getInitialState(p => ParentState(p, DND.Parent.initialState))
      .render(T => {
console.log(s"State = ${T.state}")
        val itemsState = T.focusState(_.items)((a, b) => a.copy(items = b))
        val dndState = T.focusState(_.dnd)((a, b) => a.copy(dnd = b))

        def move(from: Item, to: Item) =
          itemsState.modStateIO(DND.move(from, to, itemCmp))

        def renderItem(i: Item) =
          li(key := i.id)(RowComp((i, DND.Parent.cProps(dndState, i, itemCmp, move ))))

        div(
          h1("Drag and Drop"),
          ol(T.state.items.map(renderItem).toJsArray)

        )
      }).create
  }

  // ===================================================================================================================

//  object ReqTypes {
//
//    type CustomReqTypeId = Int
//
//    case class CustomReqType(
//        id: CustomReqTypeId,
//        mnemonic: String,
//        name: String,
//        oldMnemonics: Set[String],
//        implicationReq: Boolean,
//        alive: Boolean)
//
//    /*
//    type G = (String, Boolean)
//    type E = SpecA.E
//    type Unsaved = Option[E]
//    type SaveMap = Map[CustomReqTypeId, (P, E)]
//    type S = FormState
//    //    case class CustomReqType
//
//    val SpecA = Spec2[G, P, Modifier,  String,String,String, Boolean,Boolean,Boolean](
//      SpecSplice[P,String,String,String](_.mnemonic, MnemonicValidator).edit(TextInputEditor),
//      SpecSplice[P,Boolean,Boolean,Boolean](_.implicationReq, NopValidator).edit(CheckboxEditor),
//      x => x // (CustomIssueType.apply _).tupled)
//    )
//
//    case class FormState(saved: SaveMap, unsaved: Unsaved)
//    val savedL = SimpleLens2[FormState](_.saved)((a,b) => a.copy(saved = b))
//    val unsavedL = SimpleLens2[FormState](_.unsaved)((a,b) => a.copy(unsaved = b))
//
//    type RowId = Option[CustomReqTypeId]
//    def keyUniqueness = uniqueness[S, RowId, (CustomReqTypeId, (P, E)), String](
//      (s,w) => s.saved.toStream.filterNot(a => w.fold(false)(_ == a._1)),
//      (a,i) => i == a._2._1.mnemonic
//    )
//
//    val SpecB = Spec2X(SpecA, Some(keyUniqueness), None)
//    */
//
//    type P = CustomReqType
//    val b1 = SpecBuilder[P](
//      SpecAttr[P](_.mnemonic)(MnemonicValidator)(TextInputEditor),
//      SpecAttr[P](_.implicationReq)(NopValidator)(CheckboxEditor)
//    )
//    val b2 = b1.rowId[CustomReqTypeId]
//    b2.uniquenessCheck(_.mnemonic)
//
//    val ReqTypeTableComp = ReactComponentB[List[CustomReqType]]("ReqTypeTable")
//      .stateless
//      .render(T => {
//
//        //SpecB.forRow().render()
//
//        table(
//          thead(tr(th("Mnemonic"), th("Name"), th("Implication Required"), th("Ctrls"))),
//          tbody(
//
//          )
//        )
//
//      }).create
//
//  }
}
