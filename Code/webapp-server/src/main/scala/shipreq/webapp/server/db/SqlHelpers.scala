package shipreq.webapp.server
package db

import scala.slick.jdbc.{GetResult, SetParameter, PositionedResult, PositionedParameters}
import shipreq.base.db.SqlHelpers._
import shipreq.base.db.JodaTimeSqlHelpers._
import shipreq.taskman.api.{EmailAddr, UserId}
import shipreq.webapp.server.lib.Types._
import shipreq.webapp.server.security.PasswordAndSalt

object SqlHelpers {

  implicit val (ea1, ea2, ea3, ea4) = sqlAccessors[EmailAddr]
  implicit val (hs1, hs2, hs3, hs4) = sqlAccessors[HashedStr]
  implicit val (i81, i82, i83, i84) = sqlAccessors[ISO8601]
  implicit val (pi1, pi2, pi3, pi4) = sqlAccessors[ProjectId]
  implicit val (su1, su2, su3, su4) = sqlAccessors[ShareUrlToken]
  implicit val (ui1, ui2, ui3, ui4) = sqlAccessors[UserId]
  implicit val (um1, um2, um3, um4) = sqlAccessors[Username]

  implicit val GR_PasswordAndSalt      = GetResult(r => PasswordAndSalt.restore(r.<<, r.<<))
  implicit val GR_Project              = GetResult(r => Project(r.<<, r.<<, r.<<))
  implicit val GR_ProjectSummary       = GetResult(r => ProjectSummary(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
  implicit val GR_ResetPasswordInfo    = GetResult(r => ResetPasswordInfo(r.<<, r.<<))
  implicit val GR_UserDescriptor       = GetResult(r => UserDescriptor(r.<<, r.<<, r.<<, userRoles(r)))
  implicit val GR_UserRegistrationInfo = GetResult(r => UserRegistrationInfo(r.<<, r.<<, r.<<, r.<<))
  implicit val GR_UserSupplementalInfo = GetResult(r => UserSupplementalInfo(r.<<, r.<<))

  implicit object SP_PasswordAndSalt extends SetParameter[PasswordAndSalt] {
    def apply(v: PasswordAndSalt, pp: PositionedParameters) {
      pp.setString(v.hashedPassword)
      pp.setString(v.salt)
    }
  }

  implicit val GR_UserDetail = GetResult(r => UserDetail(r.<<, r.<<))
  implicit object SP_UserDetail extends SetParameter[UserDetail] {
    def apply(d: UserDetail, pp: PositionedParameters) {
      pp setString d.name
      pp setBoolean d.newsletter
    }
  }

  def userRoles(r: PositionedResult): Set[String] =
    r.nextStringOption() match {
      case None        => Set.empty
      case Some(roles) => roles.split(',').toSet
    }
}