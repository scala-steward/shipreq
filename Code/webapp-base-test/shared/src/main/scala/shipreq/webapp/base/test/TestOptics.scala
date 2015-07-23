package shipreq.webapp.base.test

import monocle._
import shipreq.webapp.base.data._
import shipreq.webapp.base.util.Optics

object TestOptics {

  val customReqTypesLive: Traversal[Project, Live] =
    Project.customReqTypes ^|->>
    Optics.imapTraversal   ^|->
    CustomReqType.live
}
