package shipreq.webapp.base.protocol

import monocle.macros.GenLens
import shipreq.base.util.{MMTree, UnivEq}
import shipreq.base.util.ScalaExt._
import shipreq.webapp.base.data.{TagId => Id, _}
import shipreq.webapp.base.delta.{Partition, PPI}
import shipreq.webapp.base.util.TypeclassDerivation._
import DataImplicits._

object TagProtocol {

  @inline implicit final def tcTagPov = PovTag.IdAccess

  type PovRelations = MMTree.Relations[Id]

  /** A tag and its world from its own point of view. */
  final case class PovTag(tag: Tag, rels: PovRelations) {
    @inline def id = tag.id
  }

  object PovTag {
    val tag = GenLens[PovTag](_.tag)

    object IdAccess extends ObjDataId[PovTag.type, PovTag, Id] {
      override def id(d: PovTag) = d.id
      override val unapplyData: AnyRef => Option[PovTag] = {case r: PovTag => Some(r); case _ => None}
    }
  }

  sealed trait Values

  final case class TagGroupValues(name: String,
                                  mutexChildren: MutexChildren,
                                  desc: Option[String]) extends Values

  final case class ApplicableTagValues(name: String,
                                       key: HashRefKey,
                                       desc: Option[String]) extends Values

  implicit lazy val equalValues: UnivEq[Values] = {import AutoDerive._; deriveUnivEq}

  val ppi = PPI.lens(Partition.Tags, Project.tags) { (delta, orig) =>
    var t = orig

    // Delete tags
    for (id <- delta.delete)
      t = t.mapUnderlying(_.mapValuesNow(_ removeChild id) - id)

    // Insert/update
    // (Separate phases ∵ all ids must exist before updating structure)
    t = t.addAll(delta.update.map(u => TagInTree(u.tag, Vector.empty)): _*)
    t = MMTree.ApplyRelations.trustedApplyN(t, delta.update.map(_.tmap2(_.id, _.rels)))

    t
  }

  import AutoDerive._
  implicit val tagGroupValueEquality     : UnivEq[TagGroupValues]      = deriveUnivEq
  implicit val applicableTagValueEquality: UnivEq[ApplicableTagValues] = deriveUnivEq
}