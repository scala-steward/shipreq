package shipreq.webapp.client.project.app.issues

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra._
import shipreq.webapp.base.data._
import shipreq.webapp.base.issue.Issues
import shipreq.webapp.client.project.feature.EditorFeature
import shipreq.webapp.client.project.widgets.ProjectWidgets
import shipreq.webapp.base.lib.DataReusability._

object IssuesPage {

  final case class StaticProps(pxProject: Px[Project],
                               pxProjectWidgets: Reusable[Px[ProjectWidgets.NoCtx]]) {

    val pxIssues      = pxProject.map(_.issues)
    val pxConfig      = pxProject.map(_.config).withReuse
    val pxFieldNameFn = pxConfig.map(cfg => Reusable.byRef(Field.nameByIdFromProjectConfig(cfg)))

    val component = ScalaComponent.builder[Props]("IssuesPage")
      .backend(new Backend(this, _))
      .renderBackend
      //.configure(Reusability.shouldComponentUpdate)
      .build

    @inline def render(p: Props): VdomElement = component(p)
  }

  /*
  state:
  - new editor
  - editor states (shared)
  - table view
    - sort
    - filter
    - column
   */

  final case class Props(editor: EditorFeature.ReadWrite.ForProject)

  //implicit val reusabilityProps: Reusability[Props] =
  //  Reusability.caseClass

  final class Backend(static: StaticProps, $: BackendScope[Props, Unit]) {
    import static.{component => _, render => _, _}

    def render(p: Props): VdomElement = {
      // val project = pxProject.value()
      val issues = pxIssues.value()
      if (issues.isEmpty)
        renderEmpty
      else
        renderContent(issues)
    }

    private def renderEmpty =
      <.div(
        NewIssue.render,
        EmptyBody.render)

    private def renderContent(issues: Issues) = {
      val project = pxProject.value()
      val fieldNameFn = pxFieldNameFn.value()

      <.div(
        NewIssue.render,
        Summary.Props(issues.stats, 0).render,
        // TODO Table config row (sort | filter | cols)
        Table.Props(project, issues, fieldNameFn).render)
    }
  }
}