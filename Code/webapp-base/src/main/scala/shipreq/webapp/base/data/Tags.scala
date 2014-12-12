package shipreq.webapp.base.data

import monocle.{SimpleLens, Lenser}
import scalaz.Equal
import scalaz.Isomorphism._
import shipreq.prop.util.BiMultimap
import shipreq.base.util.TaggedTypes.TaggedLong

// =====================================================================================================================
// A single tag. No relationships.

object Tag {
  final case class Id(value: Long) extends TaggedLong

  object IdAccess extends ObjDataId[Tag.type, Tag, Id] {
    override def id(d: Tag) = d.id
    override def mkId(l: Long) = Id(l)
    override def setId(t: Tag, i: Id) = t match {
      case x: TagGroup      => x.copy(id = i)
      case x: ApplicableTag => x.copy(id = i)
    }
  }

  val _name = SimpleLens[Tag](_.name)((t, n) => t match {
    case TagGroup(a, _, b, c, d)      => TagGroup(a, n, b, c, d)
    case ApplicableTag(a, _, b, c, d) => ApplicableTag(a, n, b, c, d)
  })

  val _alive = SimpleLens[Tag](_.alive)((t, n) => t match {
    case TagGroup(a, b, c, d, _)      => TagGroup(a, b, c, d, n)
    case ApplicableTag(a, b, c, d, _) => ApplicableTag(a, b, c, d, n)
  })
}

sealed trait Tag {
  val id: Tag.Id
  val name: String
  val desc: Option[String]
  val alive: Alive
  def keyO: Option[RefKey]
}

/**
 * FR-246: BA shall be able to specify that a grouping cannot be applied.
 *         e.g. “Priority” shouldn't be applicable but its children should.
 */
final case class TagGroup(id: Tag.Id,
                          name: String,
                          desc: Option[String],
                          enum: IsEnumLike,
                          alive: Alive) extends Tag {
  override def keyO = None
}

final case class ApplicableTag(id: Tag.Id,
                               name: String,
                               desc: Option[String],
                               key: RefKey,
                               alive: Alive) extends Tag {
  override def keyO = Some(key)
}

/**
 * FR-253: BA shall be able to specify that a grouping's children are mutually-exclusive (like an enum or sum-type).
 * FR-254: BA shall be able to track when two or more enum-groupings (FR-253) (or its children) are applied to the same req.
 */
sealed trait IsEnumLike
case object IsEnumLike extends IsEnumLike with (Boolean <=> IsEnumLike) {
  implicit val equal = Equal.equalA[IsEnumLike]
  override def from = _ == IsEnumLike
  override def to = b => if (b) IsEnumLike else NotEnumLike
}
case object NotEnumLike extends IsEnumLike

// =====================================================================================================================
// Many tags

final case class TagTree(tags: Map[Tag.Id, Tag], structure: TagTree.Structure) {
  @inline def parentToChild = structure.ab.m
  @inline def childToParent = structure.ba.m
}

object TagTree {
  type Structure = BiMultimap[Tag.Id, Set, Tag.Id]

  private[this] def l = Lenser[TagTree]
  val _tags = l(_.tags)
  val _structure = l(_.structure)
}

// =====================================================================================================================
// Types for sending values over the wire

object TagProtocol {

  /** A tag's relations from its own point of view. */
  final case class PovRelations(parents: Set[Tag.Id], children: Set[Tag.Id])

  /** A tag and its world from its own point of view. */
  final case class PovTag(tag: Tag, rels: PovRelations) {
    @inline def id = tag.id
  }

  object PovTag {
    private[this] def l = Lenser[PovTag]
    val _tag = l(_.tag)

    import Tag.Id
    object IdAccess extends ObjDataId[PovTag.type, PovTag, Id] {
      override def id(d: PovTag) = d.id
      override def mkId(l: Long) = Id(l)
      override def setId(t: PovTag, i: Id) = _tag.modify(t, Tag.IdAccess.setId(_, i))
    }
  }

  sealed trait Values

  final case class TagGroupValues(name: String,
                                  desc: Option[String],
                                  enum: IsEnumLike) extends Values

  final case class ApplicableTagValues(name: String,
                                       desc: Option[String],
                                       key: RefKey) extends Values
}