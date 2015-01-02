package shipreq.webapp.base.data

import monocle.macros.Lenser
import shipreq.base.util.TaggedTypes._

/**
 * A key by which users can insert references to corresponding data.
 *
 * Examples:
 * #TBD refers to a custom issue type.
 * #pri=high refers to a grouping.
 */
final case class RefKey(value: String) extends TaggedString

// =====================================================================================================================

final case class CustomIssueType(id: CustomIssueType.Id,
                                 key: RefKey,
                                 desc: Option[String],
                                 alive: Alive)

object CustomIssueType {
  final case class Id(value: Long) extends TaggedLong

  object IdAccess extends ObjDataId[CustomIssueType.type, CustomIssueType, Id] {
    override def id(d: CustomIssueType) = d.id
    override def mkId(l: Long) = Id(l)
    override def setId(a: CustomIssueType, b: Id) = a.copy(id = b)
  }

  private[this] def l = Lenser[CustomIssueType]
  val _key = l(_.key)
}