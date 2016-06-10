package shipreq.webapp.client.project.app.root

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import Routes.{Page, RouterCtl}

object LoadedHome {

  val Component = ReactComponentB[RouterCtl]("Home")
    .render_P { ctl =>
      import Page._
      <.ul(
        Vector(ReqTable, ImpGraph, CfgFields, CfgIssues, CfgReqTypes, CfgTags).map(p =>
          <.li(ctl.link(p)(p.toString))))
    }
    .build
}
