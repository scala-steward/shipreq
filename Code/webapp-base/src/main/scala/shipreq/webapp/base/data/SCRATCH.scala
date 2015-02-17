package shipreq.webapp.base.data

import japgolly.nyaya.util.Multimap
import monocle.macros.Lenser
import shipreq.base.util.{IMap, BiMap}
import shipreq.base.util.TaggedTypes._

import scalaz.{Memo, Equal, NonEmptyList}

object SCRATCH {

  // ===================================================================================================================
  // ReqCodes: A hierarchy of semantic IDs

  /**
   * A textual ID that refers to a requirement.
   *
   * Eg. "system.email.failure" would be ReqCode("failure", "email" :: "system" :: Nil)
   *
   * Each ReqCode only refers to a single target, but requirements can have 0..n ReqCodes.
   */
  final case class ReqCode(last: ReqCode.Node, secondLastToRoot: List[ReqCode.Node]) {
    def asc = (last :: secondLastToRoot).reverse
    def txt =  asc.mkString(".")
  }

  // TODO all req code text should be lowercase

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

    // ReqCodes are unique and refer to 0..1 (ReqCodeGroup | Req)
    type Trie = Map[Node, TrieNode]
    final case class TrieNode(value: Option[Target], next: Trie)

    object Trie {
      val empty: Trie = Map.empty
      def inverse(t: Trie): PerTarget = ???
    }

    // Creation = O(n)
    // Lookup   = O(log n)
    type PerTarget = Map[Target, Set[ReqCode]]
  }

  /**
   * A row that exists just to provide a description or summary of its children in the code hierarchy.
   *
   * Previously called "Semantic Header Row" or "SHR" in the requirements.
   */
  final case class ReqCodeGroup(id: ReqCodeGroup.Id, desc: String)
  object ReqCodeGroup {
    final case class Id(value: Long) extends TaggedLong with ReqCode.Target
  }

  // ===================================================================================================================
  // PublicReqIds like MF-3

  /**
   * A position (oridinal) in a req-type's ordered list of requirements.
   *
   * Eg. the "3" in "FR-3".
   *
   * @param value > 0.
   */
  final case class ReqTypePos(value: Int) extends TaggedInt

  /**
   * A requirement's ID from the public's point-of-view.
   *
   * Eg. "FR-3"
   */
  final case class PublicReqId(reqTypeId: ReqType.Id, pos: ReqTypePos)

  object PublicReqId {

    /**
     * Once a (reqtype x position) is allocated, it is never removed.
     * Thus, the 0-based position in the vector corresponds with 1-based ReqTypePos values.
     */
    type Register = Multimap[ReqType.Id, Vector, Req.Id]

    val emptyRegister: Register = Multimap.empty
  }

  // ===================================================================================================================
  // Requirements

  /** Req = GenericReq */
  sealed trait Req
  object Req {

    /** Req.Id = GenericReq.Id */
    sealed trait Id extends ReqCode.Target
  }

  final case class GenericReq(id         : GenericReq.Id,
                              pubId      : PublicReqId,
                              desc       : String,
                              // TODO lastUpdated. Need JS-compat datetimeTZ
                              alive      : Alive) extends Req {
    @inline def reqTypeId = pubId.reqTypeId
  }
  object GenericReq {
    final case class Id(value: Long) extends TaggedLong with Req.Id
  }

  // TODO ReqFieldData should be (Req)CustomFieldData
  object ReqFieldData {
    type Text         = Map[CustomField.Text.Id, Map[Req.Id, Unit]] // TODO Unit should be the rich text AST
    type Tags         = Map[Req.Id, Set[ApplicableTag.Id]]
    type Implications = BiMap[Req.Id, Req.Id]
  }

  case class ReqFieldData(text        : ReqFieldData.Text,
                          tags        : ReqFieldData.Tags,
                          implications: ReqFieldData.Implications)

}
