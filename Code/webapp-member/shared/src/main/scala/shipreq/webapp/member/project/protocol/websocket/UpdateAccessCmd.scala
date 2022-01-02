package shipreq.webapp.member.project.protocol.websocket

import shipreq.webapp.base.data.{ProjectPerm, UserId}

final case class UpdateAccessCmd(updates: Map[UserId.Public, Option[ProjectPerm]])

object UpdateAccessCmd {
  implicit def univEq: UnivEq[UpdateAccessCmd] = UnivEq.derive

  object CodecsV1 {
    import boopickle.DefaultBasic._
    import shipreq.webapp.base.protocol.binary.v1.BaseData._
    import shipreq.webapp.member.project.protocol.binary.v2.Rev0._

    implicit val picklerUpdateAccessCmd: Pickler[UpdateAccessCmd] =
      pickleMap[UserId.Public, Option[ProjectPerm]].xmap(UpdateAccessCmd.apply)(_.updates)
  }
}