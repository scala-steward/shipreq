package shipreq.webapp.client.app.ui.cfg.issues

import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import japgolly.scalajs.react.extra.OnUnmount
import scala.language.reflectiveCalls
import scalaz.effect.IO
import scalaz.syntax.equal._
import shipreq.webapp.base.data._, DataImplicits._
import shipreq.webapp.base.delta.Partition
import shipreq.webapp.base.protocol.Routines._
import shipreq.webapp.client.ClientData
import shipreq.webapp.client.lib.ui._
import shipreq.webapp.client.protocol.ClientProtocol

private[issues] object MandatoryFields {

  case class Props(cp: ClientProtocol, remote: FieldMandatorinessMod.Remote, clientData: ClientData) {
    @inline def component = Component(this)
  }

  val rowStore = SavedRowStore.data[CustomField](_.mandatory)
  import rowStore.{State => S}
  val  ST = ReactS.FixT[IO, S]
  type ST = ST.T[Unit]

  val Component = ReactComponentB[Props]("MandatoryFields")
    .getInitialState(initialState)
    .backend(new Backend(_))
    .render(_.backend.render)
//    .configure(
//      RemoteDeltaListener(Partition.Fields, _.clientData)(....) TODO
//      RemoteDeltaListener(CustomField).installS(rowStore, Partition.Fields, _.clientData)) TODO
    .build

  private def initialState(p: Props): S =
    rowStore.initStateIM(p.clientData.project.fields.data.customFields)
  
  final class Backend($: BackendScope[Props, S]) extends OnUnmount {

    @inline def project = $.props.clientData.project

    // TODO update when tags change
    var label = Field.name(project.tags.data)

    def save(p: Props, id: CustomField.Id): ST =
      ReactS.liftR[IO, S, Unit](state => {
        val setStatus = rowStore.setStatusST[IO](id)
        val saveio = Persistence.retryably[ST](retry => {
          val v = rowStore.getI(id)(state)
          val f = Persistence.failureIO(retry)($ runState _, setStatus)
          val io = $.props.cp.call(p.remote)((id, v), p.clientData.update, f)
          ST ret io
        })
        saveio >> setStatus(RowStatus.Locked)
      })

    val genEditor =
      Editors.checkboxEditor.imap(Mandatory)
        .strengthR[Field].labelSuffix(a => label(a._2))

    val editor =
      genEditor.cmapA[(Mandatory, CustomField)](a => a)
        .zoomU[S].applyRowUpdate(rowStore)(_._2.id)
        .paddSTA(a => { case OnEditFinished(_) => save($.props, a._2.id) })

    val editable = editor.editableByRowStatus($)

    def editorI(r: rowStore.Row): editor.Input =
      EditorI((r.i, r.p), "", editable(r.status))

    def renderStaticField(f: StaticField) = // Near identical
      <.tr(
        ^.key := f.name,
        <.td(genEditor render EditorI((f.mandatory, f), "", None)))

    def renderCustomField(f: CustomField) = { // Near identical
      val r = rowStore.get(f.id)($.state)
      <.tr(
        ^.key := f.id.value,
        <.td(
          editor render editorI(r),
          UI.rowStatusCtrls(r.status, EmptyTag)))
    }

    def renderRows =
      project.fields.data.fields.filter(Field.filterAlive).toReactNodeArray(
        _.fold(renderStaticField, renderCustomField))

    def render: ReactElement =
      <.table(
        <.thead(<.tr(<.th("Mandatory Fields"))),
        <.tbody(renderRows))
  }
}
