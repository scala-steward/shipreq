package shipreq.webapp.client.project.app.pages.config.fields

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shipreq.webapp.base.data._
import shipreq.webapp.base.lib.DataReusability._
import shipreq.webapp.client.project.app.Style.{fieldConfig => *}

/** This isn't really an editor; it's read/only! But it's what appears in place of the editor. */
object StaticFieldEditor {

  final case class Props(field: StaticField, cfg: ProjectConfig) {
    @inline def render: VdomElement = Component(this)
  }

  implicit val reusabilityProps: Reusability[Props] =
    Reusability.derive

  private val ul = <.ul(*.staticFieldUL)
  private val li = <.li(*.staticFieldLI)

  private val mandatory =
    li("This field is mandatory and cannot be removed.")

  private def render(p: Props): VdomNode = {
    def ul(lis: VdomNode*) =
      this.ul(
        lis.toTagMod(li(_)),
        mandatory.when(p.field.isInstanceOf[StaticField.Mandatory]))

    val body =
      p.field match {

        case StaticField.NormalAltStepTree =>
          ul("Sequences of interactions that lead to a successful outcome.")

        case StaticField.ExceptionStepTree =>
          ul(
            "Conditions that prevent a successful outcome.",
            "Sequences of interactions that describe how exceptions will be handed.")

        case StaticField.ImplicationGraph =>
          ul("A graph depicting all requirements related by implication.")

        case StaticField.StepGraph =>
          ul("A graph depicting all possible flow paths through a use case.")
      }

    // div is for tests
    <.div(body)
  }

  val Component = ScalaComponent.builder[Props]("StaticField")
    .render_P(render)
    .configure(Reusability.shouldComponentUpdate)
    .build
}
