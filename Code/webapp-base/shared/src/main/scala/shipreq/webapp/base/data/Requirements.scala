package shipreq.webapp.base.data

import nyaya.util.Multimap
import monocle.macros.Lenses
import scala.annotation.tailrec
import scalaz.{Equal, Order}
import scalaz.std.string.stringInstance
import scalaz.syntax.equal._
import shipreq.base.util._
import shipreq.base.util.ScalaExt._
import shipreq.base.util.TaggedTypes._
import shipreq.webapp.base.text.Text, Text.Equality._
import shipreq.webapp.base.util.Must._
import DataImplicits._

// ===================================================================================================================
// ReqCodes: A hierarchy of semantic IDs

final case class ReqCodeId(value: Int) extends TaggedInt

/**
 * [[ReqCode.Trie]] contains the hierarchy of codes and their targets.
 * [[ReqCodes]] is a bundle of all req-codes in a project.
 */
object ReqCode {

  case class IdAndValue(id: ReqCodeId, value: Value) {
    @inline def toTupleIV: (ReqCodeId, Value) =
      (id, value)
    @inline def toTupleVI: (Value, ReqCodeId) =
      (value, id)
  }
  implicit def idAndValueEquality: UnivEq[IdAndValue] = UnivEq.derive

  /**
   * A textual ID that refers to a requirement.
   *
   * Eg. "system.email.failure" would be `NonEmptyVector(Node("system"), Node("email"), Node("failure"))`.
   */
  type Value = NonEmptyVector[Node]

  /** For speed/mem efficiency */
  def valueToStr(v: Value, sep: Char): String = {
    val head = v.head.value
    if (v.tail.isEmpty)
      head
    else
      Util.quickSB(head, sb =>
        v.tail.foreach { n =>
          sb append sep
          sb append n.value
        }
      )
  }

  /**
   * Portion of a [[ReqCode]], separated by ".".
   *
   * Eg. "mail" in "system.mail.failure"
   */
  final class Node private (val value: String) {
    override def equals(o: Any) = o match {case b:Node => this eq b; case _ => false }
    override def hashCode = value.##
    override def toString = value
  }

  trait NodeUnivEq {
    implicit def nodeUnivEq: UnivEq[Node] = UnivEq.force
  }

  object Node extends NodeUnivEq {
    implicit val order: Order[Node] = {
      import scalaz.Ordering
      val S = Order[String]
      new Order[Node] {
        override def equal(a: Node, b: Node): Boolean =
          a eq b

        override def order(a: Node, b: Node): Ordering =
          if (a eq b)
            Ordering.EQ
          else
            S.order(a.value, b.value)
      }
    }

    val applyFn: String => Node =
      Memo(new Node(_))

    @inline def apply(value: String): Node =
      applyFn(value)
  }

  /**
   * Inactive associations to a ReqCode by reqs.
   *
   * When a req is dead, all its ReqCodes move into this.
   *
   * When a req is live, it can also contain IDs referenced in rich text that have been renamed such that they now share
   * a ReqCode (i.e. Give a req two codes [a] & [b], create refs to both, change req's codes to just [c], [c] gets [a]'s
   * ID actively and [b]'s ID inactively here).
   */
  type ReqInactive = Multimap[ReqId, Set, ReqCodeId]
  def emptyReqInactive: ReqInactive = UnivEq.emptySetMultimap

  /**
   * A [[ReqCodeGroup]] previously assigned to a ReqCode, since deleted.
   */
  type DeadGroup = Option[ReqCodeGroup.AndId]

  /**
   * Data stored at each node in the ReqCode trie.
   */
  sealed abstract class Data {
    def nonEmpty: Boolean

    @inline final def isEmpty = !nonEmpty

    def isActive: Boolean

    def activeId: Option[ReqCodeId]

    val reqInactive: ReqInactive

    def modReqInactive(f: ReqInactive => ReqInactive): Data

    def deadGroup: DeadGroup

    /** Active & inactive */
    def ids: Stream[ReqCodeId]

    protected final def _inactiveIds: Stream[ReqCodeId] =
      deadGroup.toStream.map(_.id) append reqInactive.allValues
  }

  @Lenses
  case class Inactive(deadGroup: DeadGroup, reqInactive: ReqInactive) extends Data {
    override def nonEmpty = deadGroup.nonEmpty || reqInactive.nonEmpty
    override def isActive = false
    override def activeId = None
    override def ids      = _inactiveIds
    override def modReqInactive(f: ReqInactive => ReqInactive) =
      copy(reqInactive = f(reqInactive))
  }

  @Lenses
  case class ActiveReq(id: ReqCodeId, reqId: ReqId, deadGroup: DeadGroup, reqInactive: ReqInactive) extends Data {
    override def nonEmpty = true
    override def isActive = true
    override def activeId = Some(id)
    override def ids      = id #:: _inactiveIds
    override def modReqInactive(f: ReqInactive => ReqInactive) =
      copy(reqInactive = f(reqInactive))
  }

  @Lenses
  case class ActiveGroup(groupAndId: ReqCodeGroup.AndId, reqInactive: ReqInactive) extends Data {
    @inline  def id        = groupAndId.id
    @inline  def group     = groupAndId.group
    override def nonEmpty  = true
    override def isActive  = true
    override def activeId  = Some(id)
    override def deadGroup = None
    override def ids       = id #:: _inactiveIds
    override def modReqInactive(f: ReqInactive => ReqInactive) =
      copy(reqInactive = f(reqInactive))
  }
  object ActiveGroup {
    val group = groupAndId ^|-> ReqCodeGroup.AndId.group
    val id    = groupAndId ^|-> ReqCodeGroup.AndId.id
  }

  object Data {
    val empty = Inactive(None, UnivEq.emptySetMultimap)
  }

  implicit def equalInactive   : UnivEq[Inactive]    = UnivEq.derive
  implicit def equalActiveReq  : UnivEq[ActiveReq]   = UnivEq.derive
  implicit def equalActiveGroup: UnivEq[ActiveGroup] = UnivEq.derive
  implicit def equalData       : UnivEq[Data]        = UnivEq.derive

  val  Trie = new MTrie.Types[Node, Data]
  type Trie = Trie.Trie

  val  CodeSet = new MTrie.Types[Node, Unit]
  type CodeSet = CodeSet.Trie
}

/**
 * A row that exists just to provide a description or summary of its children in the ReqCode hierarchy.
 *
 * Previously called "Semantic Header Row" or "SHR" in the requirements.
 */
@Lenses
final case class ReqCodeGroup(title: Text.ReqCodeGroupTitle.OptionalText) {
  @inline def isEmpty : Boolean = title.isEmpty
  @inline def nonEmpty: Boolean = !isEmpty

  @inline def and(id: ReqCodeId): ReqCodeGroup.AndId =
    ReqCodeGroup.AndId(id, this)

  def live = Live
}

object ReqCodeGroup {
  val empty = ReqCodeGroup(Vector.empty)

  @Lenses
  final case class AndId(id: ReqCodeId, group: ReqCodeGroup)

  implicit def equality     : UnivEq[ReqCodeGroup] = UnivEq.derive
  implicit def andIdEquality: UnivEq[AndId]        = UnivEq.derive
}

/**
 * All req code data for in a project.
 */
@Lenses
final case class ReqCodes(trie: ReqCode.Trie) {
  import ReqCode._
  import MTrie.Ops

  def apply(code: Value): Data =
    get(code) mustExistElse s"No node at reqcode ${code.whole mkString "."}."

  def get(code: Value): Option[Data] =
    trie.lookup(code)

  def lookup(id: ReqCodeId): Data =
    apply(reqCode(id))

  def reqCode(id: ReqCodeId): Value =
    reqCodesById get id mustExistElse s"No req code associated with $id."

  private lazy val scan = new Scan
  private class Scan {
    private val _allIds         = Stream.newBuilder[ReqCodeId]
    private val _activeGroups   = List.newBuilder[ReqCodeGroup.AndId]
    private val _inactiveGroups = List.newBuilder[ReqCodeGroup.AndId]
    private val _reqCodesById   = Map.newBuilder[ReqCodeId, Value]
    var _activeReqCodesByReqId: Multimap[ReqId, Set, Value] = UnivEq.emptySetMultimap
    var _inactiveIdsByReqId: Multimap[ReqId, Set, ReqCodeId] = UnivEq.emptySetMultimap

    trie.foreachPathAndValue { (code, data) =>

      val ids = data.ids
      _allIds ++= ids
      _reqCodesById ++= ids.map((_, code))

      _inactiveIdsByReqId ++= data.reqInactive.m

      data.deadGroup.map(_inactiveGroups += _)

      data match {
        case d: ActiveReq   => _activeReqCodesByReqId = _activeReqCodesByReqId.add(d.reqId, code)
        case d: ActiveGroup => _activeGroups += d.groupAndId
        case _: Inactive    => ()
      }
    }

    val activeGroups          = _activeGroups.result()
    val inactiveGroups        = _inactiveGroups.result()
    val reqCodesById          = _reqCodesById.result()
    val allIds                = _allIds.result()
    val activeReqCodesByReqId = _activeReqCodesByReqId
    val inactiveIdsByReqId    = _inactiveIdsByReqId
  }

  // TODO Are {in,}activeGroups useful really?
  // 1) (RCG,id) likely isn't enough anymore, better would be (RCG,id,live, maybe code too?)
  // 2) Logic.gather doesn't use this
  @inline def activeGroups         : List[ReqCodeGroup.AndId]        = scan.activeGroups
  @inline def inactiveGroups       : List[ReqCodeGroup.AndId]        = scan.inactiveGroups
  @inline def reqCodesById         : Map[ReqCodeId, Value]           = scan.reqCodesById
  @inline def activeReqCodesByReqId: Multimap[ReqId, Set, Value]     = scan.activeReqCodesByReqId
  @inline def inactiveIdsByReqId   : Multimap[ReqId, Set, ReqCodeId] = scan.inactiveIdsByReqId
  @inline def idStream             : Stream[ReqCodeId]               = scan.allIds

  /** Active and inactive [[ReqCodeId]]s alike. */
  lazy val idSet = idStream.toSet
}

object ReqCodes {
  implicit lazy val equality: Equal[ReqCodes] = UtilMacros.deriveEqual
  def empty: ReqCodes = ReqCodes(Map.empty)
}

// ===================================================================================================================
// Public IDs (like MF-3)

/**
 * A position (ordinal) in a req-type's ordered list of requirements.
 *
 * Eg. the "3" in "FR-3".
 *
 * @param value ≥ 1.
 */
final case class ReqTypePos(value: Int) extends TaggedInt

/**
 * Public ID: A requirement's ID from the public's point-of-view.
 *
 * Eg. "FR-3"
 */
final case class PubidT[+T <: ReqTypeId](reqTypeId: T, pos: ReqTypePos)

object PubidT {
  implicit def equality[T <: ReqTypeId : UnivEq]: UnivEq[PubidT[T]] = UnivEq.derive
}

/**
 * Once a (reqtype x position) is allocated, it is never removed.
 * Thus, the 0-based position in the vector corresponds with 1-based [[ReqTypePos]] values.
 */
case class PubidRegister(value: Multimap[ReqTypeId, Vector, ReqId]) {

  def allocC(reqTypeId: CustomReqTypeId)(reqId: ReqIdC): (PubidRegister, PubidC) =
    _alloc(reqTypeId)(reqId)

  private def _alloc[T <: ReqTypeId](reqTypeId: T)(reqId: ReqIdT[T]): (PubidRegister, PubidT[T]) = {
    val cur = value(reqTypeId)
    val i = cur.indexWhere(_ ≟ reqId)
    if (i >= 0)
      (this, PubidT(reqTypeId, ReqTypePos(i + 1)))
    else
      (PubidRegister(value.add(reqTypeId, reqId)), PubidT(reqTypeId, ReqTypePos(cur.size + 1)))
  }

   def apply[T <: ReqTypeId](id: PubidT[T]): Option[ReqIdT[T]] = {
    val v = value(id.reqTypeId)
    val i = id.pos.value - 1
    @inline def cast(r: ReqId) = r.asInstanceOf[ReqIdT[T]]
    try {
      Some(cast(v(i)))
    } catch {
      case _: IndexOutOfBoundsException => None
    }
  }
}

object PubidRegister {
  implicit def equality: UnivEq[PubidRegister] = UnivEq.derive
  def emptyMM: Multimap[ReqTypeId, Vector, ReqId] = UnivEq.emptyMultimap
  def empty = PubidRegister(emptyMM)
}

// =====================================================================================================================
// Requirements

/** type [[ReqIdT]] = [[GenericReqId]] */
sealed trait ReqIdT[+RT <: ReqTypeId] extends TaggedInt

/** [[Req]] = [[GenericReq]] */
sealed abstract class ReqT[+RT <: ReqTypeId] {
  val id: ReqIdT[RT]
  val pubid: PubidT[RT]

  def live(customReqTypes: CustomReqTypeIMap): Live

  @inline final def reqTypeId: RT =
    pubid.reqTypeId
}

object ReqT {
  implicit def equalReq(implicit g: UnivEq[GenericReq]): UnivEq[Req] = UnivEq.force

  object IdAccess extends ObjDataId[ReqT.type, Req, ReqId] {
    override def id(d: Req) = d.id
    override val unapplyData: AnyRef => Option[Req] = {case r: Req => Some(r); case _ => None}
  }
}

// ---------------------------------------------------------------------------------------------------------------------

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

  override def live(customReqTypes: CustomReqTypeIMap): Live =
    liveExplicitly match {
      case Live => customReqTypes.need(pubid.reqTypeId).live
      case Dead => Dead
    }
}

object GenericReq {
  implicit def equality: UnivEq[GenericReq] = UnivEq.derive

  object IdAccess extends ObjDataId[GenericReq.type, GenericReq, GenericReqId] {
    override def id(d: GenericReq) = d.id
    override val unapplyData: AnyRef => Option[GenericReq] = {case r: GenericReq => Some(r); case _ => None}
  }
}

// ---------------------------------------------------------------------------------------------------------------------

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
    UnivEq.emptyMultimap[ReqTypeId, Vector, Req]
      .addPairs(reqs.vstream(_.mapStrengthL(_.reqTypeId)): _*)
}