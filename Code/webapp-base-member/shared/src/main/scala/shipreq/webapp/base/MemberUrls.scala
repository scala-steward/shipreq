package shipreq.webapp.base

import shipreq.base.util.Url
import shipreq.webapp.base.data.Project

object MemberUrls {
  val home    = Url.Relative("/")
  val project = Url.Relative("/project").thenParam[Project.XId](_.value)
  val logout  = Url.Relative("/logout")
}
