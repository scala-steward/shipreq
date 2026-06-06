package shipreq.webapp.member.project.protocol.json.v2

import io.circe._
import shipreq.webapp.base.data._
import shipreq.webapp.member.project.event._
import shipreq.webapp.member.protocol.json.JsonCodec
import shipreq.webapp.member.protocol.json.JsonCodec.Implicits._

/** v2.0: For ShipReq Phase 3. */
object Rev0 {

  implicit lazy val jsonCodecUserId: JsonCodec[UserId] =
    JsonCodec.long.xmap(UserId.apply)(_.value)

  implicit lazy val decoderProjectRole: Decoder[ProjectRole] =
    Decoder.instance(c =>
      c.value.asString match {
        case Some("admin")        => Right(ProjectRole.Admin)
        case Some("collaborator") => Right(ProjectRole.Collaborator)
        case Some("viewer")       => Right(ProjectRole.Viewer)
        case _                    => Left(DecodingFailure("Invalid project role: " + c.value.noSpaces, c.history))
      }
    )

  implicit lazy val encoderProjectRole: Encoder[ProjectRole] = Encoder.instance {
    case ProjectRole.Admin        => Json.fromString("admin")
    case ProjectRole.Collaborator => Json.fromString("collaborator")
    case ProjectRole.Viewer       => Json.fromString("viewer")
  }

  implicit lazy val decoderOptionProjectRole: Decoder[Option[ProjectRole]] =
    Decoder.instance(c =>
      c.value.asString match {
        case Some("-") => Right(None)
        case _         => decoderProjectRole(c).map(Some(_))
      }
    )

  implicit lazy val encoderOptionProjectRole: Encoder[Option[ProjectRole]] = Encoder.instance {
    case None    => Json.fromString("-")
    case Some(p) => encoderProjectRole(p)
  }

  object EventData {

    implicit val decoderEventAccessUpdate: Decoder[Event.AccessUpdate] =
      Decoder.forProduct2("userId", "newRole")(Event.AccessUpdate.apply)

    implicit val encoderEventAccessUpdate: Encoder[Event.AccessUpdate] =
      Encoder.forProduct2("userId", "newRole")(a => (a.userId, a.newRole))
  }
}
