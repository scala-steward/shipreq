package shipreq.webapp.client.home.ui

import japgolly.scalajs.react.ReactMonocle._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._
import monocle.macros.Lenses
import shipreq.base.util.ErrorMsg
import shipreq.webapp.base.config.ClientConfig
import shipreq.webapp.base.feature.{AsyncFeature, ErrorHandlingFeature}
import shipreq.webapp.base.protocol.ServerSideProcInvoker
import shipreq.webapp.base.protocol.ajax.{AjaxClient, CommonProtocolsJs}
import shipreq.webapp.base.ui.semantic.Breadcrumb
import shipreq.webapp.base.ui.widgets._
import shipreq.webapp.base.util.CallbackHelpers._
import shipreq.webapp.member.project.data.{FilterDead, HideDead, ProjectMetaData}
import shipreq.webapp.member.project.util.DataReusability._
import shipreq.webapp.member.protocol.ajax.HomeSpaProtocols
import shipreq.webapp.member.protocol.entrypoint.HomeSpaEntryPoint
import shipreq.webapp.member.ui._

object Home {
  final case class Props(data: HomeSpaEntryPoint.InitData,
                         ajax: AjaxClient.Binary) {
    @inline def render = Component(this)

    def createProjectIO: ServerSideProcInvoker[String, ErrorMsg, ProjectMetaData] =
      ajax.invoker(HomeSpaProtocols.CreateProject.ajax)

    val feedbackModal: FeedbackModal = {
      val metadata = CommonProtocolsJs.Metadata.client(data.username)
      FeedbackModal(metadata, ajax)
    }
  }

  @Lenses
  final case class State(createProjectText: String,
                         createProjectAAS : AsyncFeature.Read.D0[ErrorMsg],
                         projects         : List[ProjectMetaData],
                         filterDead       : FilterDead)

  object State {

    val recorder = ErrorHandlingFeature.StateRecorder[State]

    def init(projects: List[ProjectMetaData]): State =
      State(
        createProjectText = "",
        createProjectAAS  = AsyncFeature.State.initD0,
        projects          = projects,
        filterDead        = HideDead,
      )
  }

  final class Backend($: BackendScope[Props, State]) {

    val setCreateProjectText: Reusable[SetStateFnPure[String]] =
      Reusable.fn.state($ zoomStateL State.createProjectText).setStateFn

    val setFilterDead: Reusable[SetStateFnPure[FilterDead]] =
      Reusable.fn.state($ zoomStateL State.filterDead).setStateFn

    val createProjectAF: AsyncFeature.Write.D0[ErrorMsg] =
      AsyncFeature.Write.D0.init($ zoomStateL State.createProjectAAS)

    def addProject(i: ProjectMetaData): Callback =
      $.modState(State.projects.modify(_ :+ i))

    val createProjectIO: String => Callback =
      name =>
        $.props.flatMap(p =>
          createProjectAF(
            p.createProjectIO(name)
              .rightFlatTapSync(i => setCreateProjectText.setState("") >> addProject(i))
          )
        )

    val navBarLeft: MemberNavBar.LeftProps =
      Reusable.byRef(Breadcrumb.Item.Section(ClientConfig.BreadcrumbNameMemberHome) :: Nil)

    def render(p: Props, s: State): VdomElement = {
      State.recorder.record(s)

      val navBar = MemberNavBar.Props(p.data.username, Some(p.feedbackModal), p.data.assetManifest, navBarLeft)

      def mainContent(m: TagMod): VdomElement =
        HomeContent.Props(
          s.projects,
          StateSnapshot.withReuse(s.filterDead)(setFilterDead),
          StateSnapshot.withReuse(s.createProjectText)(setCreateProjectText),
          s.createProjectAAS,
          createProjectIO,
          m)
          .render

      <.div(
        p.feedbackModal.render,
        MemberLayout.Props(navBar, mainContent).render)
    }
  }

  val Component = ScalaComponent.builder[Props]
    .initialStateCallbackFromProps(p => State.recorder.getOrElse(State.init(p.data.projects)))
    .renderBackend[Backend]
    .build
}
