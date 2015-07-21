package shipreq.webapp.client.app.ui.cfg.issues

import japgolly.scalajs.react._, vdom.prefix_<^._, ScalazReact._
import shipreq.webapp.client.lib.FilterDead
import scala.language.reflectiveCalls
import shipreq.webapp.base.protocol.RemoteFns._
import shipreq.webapp.client.ClientData
import shipreq.webapp.client.protocol.ClientProtocol

object CfgIssues {

  case class Props(cp        : ClientProtocol,
                   a         : CustomIssueTypeCrud.Instance,
                   b         : ReqTypeImplicationMod.Instance,
                   c         : FieldMandatorinessMod.Instance,
                   cd        : ClientData,
                   filterDead: FilterDead) {
    @inline def component = Component(this)
  }

  val Component =
    ReactComponentB[Props]("Cfg: Issues")
      .render(p =>
        <.section(
          <.h4("User-Defined Issue Types"),
          CustomIssueTypes.Props(p.cp, p.a, p.cd, p.filterDead).component,
          <.h4("Other Causes of Issues"),
          <.table(<.tbody(
            <.td(ReqTypeImplication.Props(p.cp, p.b, p.cd).component),
            <.td(MandatoryFields.Props(p.cp, p.c, p.cd).component)))))
      .build
}
