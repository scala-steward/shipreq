package shipreq.webapp.base.protocol

import japgolly.scalajs.react.CallbackTo
import org.scalajs.dom.window

object CommonProtocolsJs {

  object SubmitFeedback {
    import CommonProtocols.SubmitFeedback._

    def metadata(p: Option[ProjectMetadata]): CallbackTo[Metadata] =
      CallbackTo {
        Metadata(
          project   = p,
          url       = window.location.href,
          userAgent = window.navigator.userAgent,
        )
      }

    def metadataWithProject(project: CallbackTo[ProjectMetadata]): CallbackTo[Metadata] =
      project.flatMap(p => metadata(Some(p)))

    def metadataWithoutProject: CallbackTo[Metadata] =
      metadata(None)
  }

}
