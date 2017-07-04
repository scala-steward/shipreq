package shipreq.webapp.server.db

import doobie.imports._
import java.time.Instant
import shipreq.base.db.SqlHelpers._
import shipreq.webapp.base.data._
import shipreq.webapp.base.user._
import shipreq.webapp.server.logic._

object SqlHelpers {

  implicit val doobieMetaEmailAddr     = doobieMetaCaseClass[EmailAddr]
  implicit val doobieMetaIP            = doobieMetaCaseClass[IP]
  implicit val doobieMetaPasswordHash  = doobieMetaCaseClass[PasswordHash]
  implicit val doobieMetaPersonName    = doobieMetaCaseClass[PersonName]
  implicit val doobieMetaProjectId     = doobieMetaCaseClass[ProjectId]
  implicit val doobieMetaSalt          = doobieMetaCaseClass[Salt]
  implicit val doobieMetaSecurityToken = doobieMetaCaseClass[SecurityToken]
  implicit val doobieMetaUserId        = doobieMetaCaseClass[UserId]
  implicit val doobieMetaUsername      = doobieMetaCaseClass[Username]

  implicit val doobieCompositePasswordAndSalt: Composite[PasswordAndSalt] =
    Composite.generic
//    Composite[(PasswordHash, Salt)].xmap[PasswordAndSalt](
//      p => PasswordAndSalt(p._1, p._2),
//      v => (v.hashedPassword, v.salt.toBase64))

//  implicit val doobieCompositePasswordAndSalt =
//    Composite[(PasswordHash, String)].xmap[PasswordAndSalt](
//      p => PasswordAndSalt(p._1, Salt.fromBase64(p._2)),
//      v => (v.hashedPassword, v.salt.toBase64))

//  implicit val doobieCompositeUserRegistration: Composite[DB.UserRegistration] =
//    Composite[(PasswordHash, String)].xmap[PasswordAndSalt](
//      p => PasswordAndSalt(p._1, Salt.fromBase64(p._2)),
//      v => (v.hashedPassword, v.salt.toBase64))

//  implicit val doobieCompositeUserRegistrationInfo: Composite[DB.UserRegistration] =
//    Composite.generic
//
//  implicit val doobieCompositePasswordResetState: Composite[DB.PasswordResetState] =
//    Composite.generic

  implicit val doobieCompositeUserDescriptor: Composite[User] =
    Composite[(UserId, Username, EmailAddr, Option[String])]
      .readOnly(r => User(r._1, r._2, r._3, userRoles(r._4)))

  def userRoles(r: Option[String]): Set[String] =
    r match {
      case None        => Set.empty
      case Some(roles) => roles.split(',').toSet
    }

  implicit val doobieCompositeProjectMetaData: Composite[ProjectMetaData] =
    Composite[(ProjectId, String, Int, Int, Instant, Option[Instant])].readOnly {
      case(id, name, evCount, reqCount, createdAt, lastUpdatedAt) =>
        ProjectMetaData(ProjectId.Extern(id), name unNull "", evCount, reqCount, createdAt, lastUpdatedAt)
    }

}