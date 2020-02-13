package shipreq.webapp.server.db

import doobie.imports._
import doobie.postgres.imports._
import japgolly.microlibs.adt_macros.AdtMacros
import japgolly.univeq.UnivEq
import shipreq.base.db.DoobieHelpers._
import shipreq.webapp.base.data._
import shipreq.webapp.base.user._
import shipreq.webapp.server.logic._

object DbMeta {

  implicit val doobieAtomResponseType: Atom[ResponseType] =
    pgEnumString[ResponseType]("response_type", _ => ???, _.dbValue)

  implicit val doobieMetaEmailAddr: Meta[EmailAddr] =
    meta1(EmailAddr.apply)(_.value)

  implicit val doobieMetaIP: Meta[IP] =
    meta1(IP.apply)(_.value)

  implicit val doobieMetaPasswordHash: Meta[PasswordHash] =
    meta1(PasswordHash.apply)(_.value)

  implicit val doobieMetaPersonName: Meta[PersonName] =
    meta1(PersonName.apply)(_.value)

  implicit val doobieMetaProjectId: Meta[ProjectId] =
    meta1(ProjectId.apply)(_.value)

  implicit val doobieMetaSalt: Meta[Salt] =
    meta1(Salt.apply)(_.base64)

  implicit val doobieMetaVerificationToken: Meta[VerificationToken] =
    meta1(VerificationToken.apply)(_.value)

  implicit val doobieMetaUserId: Meta[UserId] =
    meta1(UserId.apply)(_.value)

  implicit val doobieMetaUsername: Meta[Username] =
    meta1(Username.apply)(_.value)

  implicit val doobieCompositePasswordAndSalt: Composite[PasswordAndSalt] =
    Composite.generic

  implicit val doobieCompositeUser: Composite[User] = {
    Composite[(UserId, Username)].readOnly(r => User(r._1, r._2))
  }

}

/** @since DB migration v4.4 */
sealed abstract class ResponseType(final val dbValue: String, final val idx: Int)
object ResponseType {
  case object `1xx` extends ResponseType("1xx", 0)
  case object `2xx` extends ResponseType("2xx", 1)
  case object `3xx` extends ResponseType("3xx", 2)
  case object `4xx` extends ResponseType("4xx", 3)
  case object `5xx` extends ResponseType("5xx", 4)
  case object Other extends ResponseType("other", 5)

  def apply(code: Int): ResponseType =
    if (code >= 200) {
      if (code < 300)
        `2xx`
      else if (code < 400)
        `3xx`
      else if (code < 500)
        `4xx`
      else if (code < 600)
        `5xx`
      else
        Other
    } else {
      if (code >= 100)
        `1xx`
      else
        Other
    }

  implicit def univEq: UnivEq[ResponseType] = UnivEq.derive

  val values = AdtMacros.adtValues[ResponseType]

  assert(values.whole.indices.toSet == values.whole.map(_.idx).toSet)
}
