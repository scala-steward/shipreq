package shipreq.webapp.base

import shipreq.webapp.base.data.{ExternalId, Project}

object URLs {
  def login                   : String = "/login"
  def logout                  : String = "/logout"
  def publicHome              : String = "/"
  def memberHome              : String = "/"
  def project(id: Project.XId): String = "/project/" + id.value

  /** This is for Lift in webapp-server and will be DCE'd from JS */
  object ForLift {
    private def toLift(s: String): String = {
      val ss = s.replaceFirst("^/", "")
      if (ss.isEmpty) "index" else ss
    }
    def login     : String = toLift(URLs.login)
    def logout    : String = toLift(URLs.logout)
    def publicHome: String = toLift(URLs.publicHome)
    def memberHome: String = toLift(URLs.memberHome)
    def project   : String = toLift(URLs.project(ExternalId(""))).replaceFirst("/.*", "")
  }
}
