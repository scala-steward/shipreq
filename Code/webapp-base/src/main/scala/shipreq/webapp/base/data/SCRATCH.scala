package shipreq.webapp.base.data

import japgolly.nyaya.util.Multimap
import monocle.macros.Lenser
import shipreq.base.util.TaggedTypes._

import scalaz.{Equal, NonEmptyList}

object SCRATCH {

  /**
   * A textual ID that refers to a requirement.
   *
   * Each ReqCode only refers to a single target, but requirements can have 0..n ReqCodes.
   */
  final case class ReqCode(head: ReqCode.Node, tail: Vector[ReqCode.Node])
  object ReqCode {

    /* TODO Make ReqCode.Node memory-efficient
    final class ReqCodeNode private (val value: String) {
      //override def equals(o: Any) = o match {case b:ReqCodeNode }
      override def hashCode = value.##
      override def toString = s"ReqCodeNode($value)"
    }
    object ReqCodeNode {
      implicit val equality: Equal[ReqCodeNode] = Equal.equalRef

      private[this] val cache = new java.util.HashMap[String, ReqCodeNode](128)

      def apply(value: String): ReqCodeNode = {
        println("yarrrrrrrr")
        var r = cache.get(value)
        if (null == r)
          synchronized {
            r = cache.get(value) // unnecessary in JS
            if (null == r)
              r = cache.put(value, new ReqCodeNode(value))
          }
        r
      }
    }
    */
    /**
     * Portion of a ReqCode, separated by ".".
     *
     * Eg. "mail" in "system.mail.failure"
     */
    final case class Node(value: String) extends TaggedString

    /**
     * Something to which a ReqCode can refer.
     *
     * Target = ReqCodeGroup.Id | Req.Id
     */
    sealed trait Target

    /*
    // ReqCodes are unique → ReqCodeGroup | Req
    type Trie = Map[Node, TrieNode]
    object Trie {
      val empty: Trie = Map.empty
    }
    final case class TrieNode(value: Option[Target], next: Trie)
    */

  }

  final case class ReqCodeGroup(id: ReqCodeGroup.Id, code: ReqCode, desc: String)
  object ReqCodeGroup {
    final case class Id(value: Long) extends TaggedLong with ReqCode.Target
  }

  // ---------------------------------------------------------------

  /**
   * A position (oridinal) in a req-type's ordered list of requirements.
   *
   * Eg. the "3" in "FR-3".
   */
  final case class ReqTypePos(value: Int) extends TaggedInt

  /**
   * A requirement's ID from the public's point-of-view.
   *
   * Eg. "FR-3"
   */
  final case class PublicReqId(reqTypeId: ReqType.Id, pos: ReqTypePos)

  // ∀ k:PublicReqId ∃! Option[Req]
  type AllPublicReqIds = Multimap[ReqType.Id, Vector, Req.Id]

  // ---------------------------------------------------------------

  // TODO FieldValues
  // type FieldValues = Map[Field.Id, ?] -- nope. Think of how tag fields work.
  // case class FieldValues(textFields: Map[CustomField.(Text)Id, TextAST], tags: Vector[Tag.Id])
  type FieldValues = Unit


  /** Req = GenericReq */
  sealed trait Req
  object Req {

    /** Req.Id = GenericReq.Id */
    sealed trait Id extends ReqCode.Target
  }

  final case class GenericReq(id         : GenericReq.Id,
                              pubId      : PublicReqId,
                              codes      : Set[ReqCode], // not here
                              desc       : String,
                              fieldValues: FieldValues,
                              // TODO lastUpdated. Need JS-compat datetimeTZ
                              alive      : Alive) extends Req
  object GenericReq {
    final case class Id(value: Long) extends TaggedLong with Req.Id
  }

}
