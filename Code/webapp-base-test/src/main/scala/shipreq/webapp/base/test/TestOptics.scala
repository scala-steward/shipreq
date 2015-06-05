package shipreq.webapp.base.test

import monocle._
import shipreq.webapp.base.data._
import shipreq.webapp.base.util.Optics

object TestOptics {

  val customReqTypesLive: Traversal[Project, Live] =
    Project.customReqTypes ^|->
    RevAnd.data            ^|->>
    Optics.imapTraversal   ^|->
    CustomReqType.live

  lazy val projectRevs: Setter[Project, Rev] = {
    val t1 = Project.customIssueTypes ^|-> RevAnd.rev asSetter
    val t2 = Project.customReqTypes   ^|-> RevAnd.rev asSetter
    val t3 = Project.fields           ^|-> RevAnd.rev asSetter
    val t4 = Project.tags             ^|-> RevAnd.rev asSetter
    val t5 = Project.reqs             ^|-> RevAnd.rev asSetter
    val t6 = Project.reqCodes         ^|-> RevAnd.rev asSetter
    val t7 = Project.reqFieldData     ^|-> RevAnd.rev asSetter

    Optics.compositeSetters(t1, t2, t3, t4, t5, t6, t7)
  }
}
