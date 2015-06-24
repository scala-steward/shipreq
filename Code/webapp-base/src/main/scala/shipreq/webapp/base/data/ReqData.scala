package shipreq.webapp.base.data

import japgolly.nyaya.CycleDetector
import japgolly.nyaya.util.Multimap
import shipreq.base.util.UnivEq
import shipreq.webapp.base.text.Text
import shipreq.webapp.base.util.TransitiveClosure
import shipreq.webapp.base.util.TypeclassDerivation._
import Text.Equality._

/**
 * Data attributed to requirements beyond their basic definitions.
 */
object ReqData {

  type Text = Map[CustomField.Text.Id, Map[ReqId, Text.CustomTextField.NonEmptyText]]

  def emptyText: Text = Map.empty

  implicit def equalityText: UnivEq[Text] = UnivEq.map

  // -------------------------------------------------------------------------------------------------------------------

  type Tags = Multimap[ReqId, Set, ApplicableTagId]

  def emptyTags: Tags = Multimap.empty

  implicit def equalityTags: UnivEq[Tags] = UnivEq.multimap

  // -------------------------------------------------------------------------------------------------------------------

  /** U = Unidirectional */
  type ImplicationsU = Multimap[ReqId, Set, ReqId]

  case class Implications(srcToTgt: ImplicationsU) {
    lazy val tgtToSrc: ImplicationsU = srcToTgt.reverse

    def members: Set[ReqId] =
      srcToTgt.m.toStream.foldLeft(UnivEq.emptySet[ReqId]) {
        case (q, (k, vs)) => q + k ++ vs
      }
  }

  def implicationCycleDetector =
    CycleDetector.Directed.multimap[Set, ReqId, Long](_.value, UnivEq.emptySet)

  def implicationTransitiveClosure(keys: Iterable[ReqId], dead: Set[ReqId], is: ImplicationsU): TransitiveClosure[ReqId] =
    TransitiveClosure.auto[ReqId](keys)(is.apply, !dead.contains(_))

  def emptyImplicationsU: ImplicationsU = Multimap.empty
  def emptyImplications = Implications(emptyImplicationsU)

  implicit def equalityI: UnivEq[Implications] = deriveUnivEq
}

