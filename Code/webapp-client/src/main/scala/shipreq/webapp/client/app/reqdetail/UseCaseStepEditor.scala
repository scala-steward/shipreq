package shipreq.webapp.client.app.reqdetail

import shipreq.webapp.client.app.Style.reqdetail.{useCaseStep => *}
import scalacss.ScalaCssReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.extra._
import shipreq.base.util.{Direction, VectorTree, univEqOps}
import shipreq.webapp.base.data._
import shipreq.webapp.base.protocol.UpdateContentCmd
import shipreq.webapp.client.widgets.high.ProjectWidgets
import shipreq.webapp.client.feature.{AsyncActionFeature, ContentEditorFeature}
import shipreq.webapp.client.lib.DataReusability._

object UseCaseStepEditor {
  AsyncActionFeature

  case class Props(pos       : ReqTypePos,
                   loc       : VectorTree.Location,
                   step      : UseCaseStep,
                   flow      : UseCases.StepFlow,
                   stepLabel : UseCaseStepId ~=> String,
                   field     : StaticField.UseCaseStepTree,
                   widgets   : ProjectWidgets,
                   editState : ContentEditorFeature.D0.State,
                   asyncState: AsyncActionFeature.D0.State[String],
                   startEdit : Callback,
                   update    : UpdateContentCmd.ForUseCaseStep => Callback) {
    @inline def id = step.id
    @inline def render = Component.withKey(step.id.value)(this)
  }

  implicit val propsReuse: Reusability[Props] = {
    val most = Reusability.caseClassExcept[Props]('flow, 'startEdit, 'update)

    val flow = Reusability.fn[Props]((x, y) =>
      (x.flow eq y.flow) || Direction.forall(d => x.flow(d)(x.id) ==* y.flow(d)(y.id)))

    most && flow
  }

  final class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) = {

      def header =
        p.field.stepLabel(p.pos, p.loc, mnemonicPrefix = false)

      def body = {
        // TODO Missing flow in text --------------------------------------------------------------------------------
        def view: ReactNode =
          p.widgets.format(Live, p.step.title) // TODO Live? ----------------------------------------
        p.asyncState renderOr (p.editState renderOr view)
      }

      def ctrls = {
        import UpdateContentCmd._
        // TODO Cache all four
        def b(label: String, cmd: => ForUseCaseStep) =
          <.button(
            *.ctrl,
            ^.onClick --> Callback.lazily(p update cmd),
            label)

        TagMod(
          b("-", DeleteUseCaseStep(p.id)),
          b("«", ShiftUseCaseStepLeft(p.id)),
          b("»", ShiftUseCaseStepRight(p.id)),
          b("+", AddUseCaseStep(???, p.field, p.loc.whole))) // TODO ----------------------------------------
      }

      <.div(*.container,
        <.div(*.header(p.loc.length - 1), header),
        <.div(*.body, body),
        <.div(*.ctrls, ctrls))
    }
  }

  val Component = ReactComponentB[Props]("UseCaseStep")
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build
}
