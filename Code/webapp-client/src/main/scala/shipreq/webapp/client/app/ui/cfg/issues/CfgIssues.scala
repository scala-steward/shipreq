package shipreq.webapp.client.app.ui.cfg.issues

import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import scala.language.reflectiveCalls
import shipreq.webapp.base.protocol.Routines._
import shipreq.webapp.client.ClientData
import shipreq.webapp.client.protocol.ClientProtocol

object CfgIssues {

  case class Props(cp         : ClientProtocol,
                   a          : CustomIssueTypeCrud.Remote,
                   b          : ReqTypeImplicationMod.Remote,
                   c          : FieldMandatorinessMod.Remote,
                   cd         : ClientData,
                   showDeleted: Boolean) {
    @inline def component = Component(this)
  }

  val Component =
    ReactComponentB[Props]("Cfg: Issues")
      .render(* =>
        <.section(
          <.h4("User-Defined Issue Types"),
          CustomIssueTypes.Props(*.cp, *.a, *.cd, *.showDeleted).component,
          <.h4("Other Causes of Issues"),
          <.table(<.tbody(
            <.td(ReqTypeImplication.Props(*.cp, *.b, *.cd).component),
            <.td(MandatoryFields.Props(*.cp, *.c, *.cd).component)))))
      .build
}
