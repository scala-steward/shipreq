package shipreq.webapp.base

import shipreq.webapp.base.data.{ExternalId, Project}

object URLs {
  def logout = "/logout"
  def memberHome = "/"
  def project(id: Project.XId) = "/project/" + id.value

  /** This is for Lift in webapp-server and will be DCE'd from JS */
  object ForLift {
    private def toLift(s: String): String = {
      val ss = s.replaceFirst("^/", "")
      if (ss.isEmpty) "index" else ss
    }
    def logout = toLift(URLs.logout)
    def memberHome = toLift(URLs.memberHome)
    def project = toLift(URLs.project(ExternalId(""))).replaceFirst("/.*", "")
  }
}
