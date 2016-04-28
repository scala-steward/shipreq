package shipreq.webapp.server.db

import org.joda.time.DateTime

case class ResetPasswordInfo(token: Option[String], sentAt: Option[DateTime])

case class UsrCount(registered: Long, total: Long) {
  def pending = total - registered
}

