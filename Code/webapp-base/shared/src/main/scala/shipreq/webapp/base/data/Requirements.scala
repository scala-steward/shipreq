package shipreq.webapp.base.data

import nyaya.util.Multimap
import monocle.macros.Lenses
import scalaz.Equal
import shipreq.base.util._
import shipreq.base.util.ScalaExt._
import shipreq.base.util.TaggedTypes._
import shipreq.webapp.base.text.Text, Text.Equality._
import shipreq.webapp.base.util.Must._
import DataImplicits._

/**
 * The ID of a requirement.
 *
 * The `T` suffix means typed with `ReqId` being `ReqIdT[_]`.
 */
sealed trait ReqIdT[+RT <: ReqTypeId] extends TaggedInt

/**
 * An abstract requirement.
 *
 * The `T` suffix means typed with `Req` being `ReqT[_]`.
 */
sealed abstract class ReqT[+RT <: ReqTypeId] {
  val id: ReqIdT[RT]
  val pubid: PubidT[RT]

  def live(customReqTypes: CustomReqTypeIMap): Live

  @inline final def reqTypeId: RT =
    pubid.reqTypeId
}

object ReqT {
  object IdAccess extends ObjDataId[ReqT.type, Req, ReqId] {
    override def id(d: Req) = d.id
    override val unapplyData: AnyRef => Option[Req] = {case r: Req => Some(r); case _ => None}
  }
}

// =====================================================================================================================
// Generic

final case class GenericReqId(value: Int) extends TaggedInt with ReqIdT[CustomReqTypeId]

/**
 * A generic/low-level requirement comprised, primarily, of a custom req type and a title.
 *
 * @param liveExplicitly Whether the user has explicitly marked this req as deleted or not.
 */
@Lenses
final case class GenericReq(id            : GenericReqId,
                            pubid         : PubidC,
                            title         : Text.GenericReqTitle.OptionalText,
                            liveExplicitly: Live) extends ReqT[CustomReqTypeId] {

  import GenericReq.ImplicitLiveStatus

  def implicitLiveStatus(customReqTypes: CustomReqTypeIMap): ImplicitLiveStatus =
    customReqTypes.need(pubid.reqTypeId).live match {
      case Live => ImplicitLiveStatus.NoImpact
      case Dead => ImplicitLiveStatus.ReqTypeIsDead
    }

  override def live(customReqTypes: CustomReqTypeIMap): Live =
    liveExplicitly && implicitLiveStatus(customReqTypes).live
}

object GenericReq {
  implicit def equality: UnivEq[GenericReq] = UnivEq.derive

  object IdAccess extends ObjDataId[GenericReq.type, GenericReq, GenericReqId] {
    override def id(d: GenericReq) = d.id
    override val unapplyData: AnyRef => Option[GenericReq] = {case r: GenericReq => Some(r); case _ => None}
  }

  /**
   * In order for a requirement to be live, its dependencies must allow it.
   *
   * This encodes the impact of a requirement's dependencies on its live status.
   */
  sealed trait ImplicitLiveStatus {
    def live: Live
  }
  object ImplicitLiveStatus {
    case object NoImpact      extends ImplicitLiveStatus { override def live = Live }
    case object ReqTypeIsDead extends ImplicitLiveStatus { override def live = Dead }
  }
}

// =====================================================================================================================
// Collective

object Requirements {
  def empty = Requirements(emptyDataMap(GenericReq), PubidRegister.empty)

  implicit lazy val equality: Equal[Requirements] = UtilMacros.deriveEqual
}

@Lenses
case class Requirements(genericReqs: GenericReqIMap, pubids: PubidRegister) {

  val reqs: IMap[ReqId, Req] =
    // Temporary. Will do this properly when next Req type added
    genericReqs.asInstanceOf[IMap[ReqId, Req]]

  def isEmpty = reqs.isEmpty
  def nonEmpty = !isEmpty

  def getReq[T <: ReqTypeId](id: ReqIdT[T]): Option[ReqT[T]] =
    id match {
      case i: GenericReqId => genericReqs.get(i)
    }

  def getReqByPubid[T <: ReqTypeId](id: PubidT[T]): Option[ReqT[T]] =
    pubids(id) flatMap getReq

  def req[T <: ReqTypeId](id: ReqIdT[T]): ReqT[T] =
    getReq(id) mustExistElse s"Req $id not found."

  def reqByPubid[T <: ReqTypeId](id: PubidT[T]): ReqT[T] =
    getReqByPubid(id) mustExistElse s"Req for $id not found."

  def reqIdByPubid[T <: ReqTypeId](id: PubidT[T]): ReqIdT[T] =
    pubids(id) mustExistElse s"Req for $id not found."

  lazy val reqsByType: Multimap[ReqTypeId, Vector, Req] =
    reqs.valuesIterator.foldLeft(UnivEq.emptyMultimap[ReqTypeId, Vector, Req])((q, r) =>
      q.add(r.reqTypeId, r))
}