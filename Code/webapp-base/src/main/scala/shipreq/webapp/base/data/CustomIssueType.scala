package shipreq.webapp.base.data

import monocle.macros.GenLens
import shipreq.base.util.TaggedTypes._

final case class CustomIssueType(id: CustomIssueType.Id,
                                 key: HashRefKey,
                                 desc: Option[String],
                                 alive: Alive)

object CustomIssueType {
  final case class Id(value: Long) extends TaggedLong

  object IdAccess extends ObjDataId[CustomIssueType.type, CustomIssueType, Id] {
    override def id(d: CustomIssueType) = d.id
    override val unapplyData: AnyRef => Option[CustomIssueType] = {case r: CustomIssueType => Some(r); case _ => None}
  }

  val key = GenLens[CustomIssueType](_.key)
}