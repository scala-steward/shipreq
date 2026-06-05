package shipreq.webapp.client.home.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shipreq.base.util.ErrorMsg
import shipreq.webapp.base.config.WebappConfig
import shipreq.webapp.base.feature.{AsyncFeature, EditorStatus}
import shipreq.webapp.base.ui.semantic.Colour
import shipreq.webapp.base.ui.widgets._
import shipreq.webapp.member.project.data.{DataValidators, FilterDead, ProjectMetaData}
import shipreq.webapp.member.ui._

object HomeContent {

  final case class Props(projects         : List[ProjectMetaData],
                         filterDead       : StateSnapshot[FilterDead],
                         createProjectText: StateSnapshot[String],
                         createProjectAS  : AsyncFeature.Read.D0[ErrorMsg],
                         createProjectIO  : String => Callback,
                         tagMod           : TagMod) {
    @inline def render = Component(this)
  }

  final class Backend {

    val inputMod: TagMod =
      TagMod(^.placeholder := "New project name", Styles.createProjectInput)

    def render(p: Props): VdomElement = {

      val noProjects = p.projects.isEmpty

      val topRow = {

        val status: EditorStatus =
          EditorStatus.async(p.createProjectAS) getOrElse
            EditorStatus.ignoreOrValidate(DataValidators.projectName.unnamed)(
              p.createProjectText.value, _.isEmpty, s => Some(p.createProjectIO(s)))

        val createProject =
          PlainTextEditor.WithButton.Props(
            p.createProjectText.value,
            p.createProjectText.setState,
            status,
            Colour.Green,
            buttonLabel = "Create Project",
            inputMod = TagMod(inputMod, (^.autoFocus := true).when(noProjects)),
          ).render

        val filterDeadButton =
          FilterDeadButton.Component(p.filterDead)

        <.div(Styles.createProjectCont,
          createProject,
          <.div(filterDeadButton))
      }

      def noProjectGreeting: VdomTag =
        <.div(Styles.noProjects,
          NoContentMessage(
            s"Welcome to ${WebappConfig.appName}!",
            TagMod(
              "The first thing you'll want to do is create a project to contain all of your requirements.",
              <.br,
              "Create a new project using the button above.")))

      def projectList: VdomTag = {
        val filtered = p.filterDead.value(p.projects)(_.live)
        <.div(Styles.projectList,
          filtered.sortBy(_.name).toTagMod(p => ProjectItem.AsLink.Component.withKey(p.id.value)(p)))
      }

      <.main(
        BaseStyles.containerLarge,
        p.tagMod,
        topRow,
        if (noProjects) noProjectGreeting else projectList)
    }
  }

  val Component = ScalaComponent.builder[Props]
    .renderBackend[Backend]
    .build

}
