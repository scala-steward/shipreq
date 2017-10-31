package shipreq.webapp.base.hash2

import japgolly.microlibs.nonempty.NonEmptyVector
import shipreq.webapp.base.data.Project

final class HashSchemes(schemes: NonEmptyVector[HashScheme]) {

  val latest: HashScheme =
    schemes.last

  val latestId: HashSchemeId =
    HashSchemeId.zero.plus(schemes.length - 1)

  private[this] val allWhole = schemes.whole

  def unsafeGet(id: HashSchemeId): HashScheme =
    allWhole(id.value - HashSchemeId.zero.value)

  import HashSchemes.EvolutionOp

  private[hash2] def addEvolution(op1: EvolutionOp, opN: EvolutionOp*): HashSchemes = {
    val newScheme: HashScheme =
      (op1 +: opN).foldLeft(latest) { (cur, op) =>

        def assertScopeExists(s: HashScope) = assert(cur.hashFns.contains(s), s"Evolution error! Scheme doesn't contain scope: $s")
        def assertScopeDoesntExist(s: HashScope) = assert(!cur.hashFns.contains(s), s"Evolution error! Scheme already contains scope: $s")

        op match {

          case EvolutionOp.Add((s, h)) =>
            assertScopeDoesntExist(s)
            HashScheme(cur.hashFns.updated(s, h))

          case EvolutionOp.Evolve((s, h)) =>
            assertScopeExists(s)
            HashScheme(cur.hashFns.updated(s, h))

          case EvolutionOp.Drop(s) =>
            assertScopeExists(s)
            HashScheme(cur.hashFns - s)
        }
      }

    new HashSchemes(schemes :+ newScheme)
  }
}

// ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

object HashSchemes {

  private sealed trait EvolutionOp

  private object EvolutionOp {
    case class Add   (kv: (HashScope, HashFn[Project])) extends EvolutionOp
    case class Evolve(kv: (HashScope, HashFn[Project])) extends EvolutionOp
    case class Drop  (k: HashScope)                     extends EvolutionOp
  }

  import EvolutionOp._

  private def init(first: HashScheme): HashSchemes =
    new HashSchemes(NonEmptyVector one first)

  val all: HashSchemes =
    init(
      HashScheme(Map(
        HashScope.ProjectName     --> ProjectHasher.hashProjectName,
        HashScope.CfgIssueTypes   --> ProjectHasher.hashCustomIssueTypes,
        HashScope.CfgReqTypes     --> ProjectHasher.hashReqTypes,
        HashScope.CfgFields       --> ProjectHasher.hashFieldSet,
        HashScope.CfgTags         --> ProjectHasher.hashTagTree,
        HashScope.GenericReqs     --> ProjectHasher.hashGenericReqs,
        HashScope.UseCases        --> ProjectHasher.hashUseCases,
        HashScope.PubidRegister   --> ProjectHasher.hashPubidRegister,
        HashScope.ReqCodes        --> ProjectHasher.hashReqCodes,
        HashScope.TextFieldData   --> ProjectHasher.hashReqDataText,
        HashScope.TagData         --> ProjectHasher.hashReqDataTags,
        HashScope.ImplicationData --> ProjectHasher.hashImplications,
        HashScope.DeletionReasons --> ProjectHasher.hashDeletionReasons,
        HashScope.SavedViews      --> ProjectHasher.hashSavedViews,
      )))

}
