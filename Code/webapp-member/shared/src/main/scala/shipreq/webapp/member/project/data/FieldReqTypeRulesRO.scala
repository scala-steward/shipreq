package shipreq.webapp.member.project.data

import cats.Applicative
import japgolly.microlibs.stdlib_ext.StdlibExt._
import monocle.Traversal
import shipreq.base.util.{Applicability, Applicable, NotApplicable => NA}
import shipreq.webapp.member.project.data.FieldReqTypeRulesRO._

/** Rules for read-only fields settings per req type.
 *
 * Eg.
 *
 *     [ CO SI ] -- Not applicable
 *     [ XX    ] -- Default to #new1
 *     Otherwise -- Default to #new2
 */
final case class FieldReqTypeRulesRO[+D](perReqType: Map[ReqTypeId, Resolution[D]], otherwise: Resolution[D]) {

  def apply(id: ReqTypeId): Resolution[D] =
    perReqType.getOrElse(id, otherwise)

  def -(id: ReqTypeId): FieldReqTypeRulesRO[D] =
    FieldReqTypeRulesRO(perReqType - id, otherwise)

  def resolutionIterator(): Iterator[Resolution[D]] =
    perReqType.valuesIterator ++ Iterator.single(otherwise)

  def resolutionIterator(reqTypeFilter: ReqTypeId => Boolean): Iterator[Resolution[D]] =
    Iterator.single(otherwise) ++ perReqType.iterator.filter(x => reqTypeFilter(x._1)).map(_._2)

  def liveResolutionIterator(reqTypes: ReqTypes): Iterator[Resolution[D]] =
    resolutionIterator(reqTypes.live(_, Dead) is Live)

  def liveIterator(reqTypes: ReqTypes): Iterator[(ReqType, Resolution[D])] =
    reqTypes.all
      .iterator
      .filter(_.live is Live)
      .map(r => (r, apply(r.reqTypeId)))

  def foreach(f: (Option[ReqTypeId], Resolution[D]) => Unit): Unit = {
    for ((rt, res) <- perReqType)
      f(Some(rt), res)
    f(None, otherwise)
  }

  def modResolutions[DD >: D](f: Resolution[DD] => Resolution[DD]): FieldReqTypeRulesRO[DD] =
    FieldReqTypeRulesRO.resolutionTraversal[DD].modify(f)(this)

  def updated[DD >: D](reqTypeId: Option[ReqTypeId], res: Resolution[DD]): FieldReqTypeRulesRO[DD] =
    reqTypeId match {
      case Some(id) => copy(perReqType.updated(id, res))
      case None     => copy(otherwise = res)
    }

  def updated[DD >: D](ids: ReqTypeId*)(res: Resolution[DD]): FieldReqTypeRulesRO[DD] =
    copy(ids.foldLeft(perReqType: Map[ReqTypeId, Resolution[DD]])(_.updated(_, res)))

  def defaultTo[DD >: D](d: DD)(ids: ReqTypeId*): FieldReqTypeRulesRO[DD] =
    updated[DD](ids: _*)(Resolution.DefaultTo(d))

  def notApplicable(ids: ReqTypeId*): FieldReqTypeRulesRO[D] =
    updated(ids: _*)(Resolution.NotApplicable)

  def hardDelete(id: ReqTypeId): FieldReqTypeRulesRO[D] =
    if (perReqType contains id)
      FieldReqTypeRulesRO(perReqType - id, otherwise)
    else
      this

  private[data] def byResolution[DD >: D]: FieldReqTypeRulesRO.ByResolution[DD] = {
    var perRes = Map.empty[Resolution[DD], NonEmptySet[ReqTypeId]]
    for ((id, res) <- perReqType) {
      val newIds = perRes.get(res) match {
        case Some(ids) => ids + id
        case None      => NonEmptySet one id
      }
      perRes = perRes.updated(res, newIds)
    }
    ByResolution(perRes, otherwise)
  }
}

object FieldReqTypeRulesRO {

  def resolutionTraversal[D]: Traversal[FieldReqTypeRulesRO[D], Resolution[D]] =
    new Traversal[FieldReqTypeRulesRO[D], Resolution[D]] {
      override def modifyA[F[_]](f: Resolution[D] => F[Resolution[D]])(s: FieldReqTypeRulesRO[D])(implicit F: Applicative[F]): F[FieldReqTypeRulesRO[D]] = {
        val fMap: F[Map[ReqTypeId, Resolution[D]]] =
          s.perReqType
            .iterator
            .map { case (k, v) => F.map(f(v))((k, _)) }
            .foldLeft(F.pure(Map.empty[ReqTypeId, Resolution[D]]))(F.map2(_, _)(_ + _))

        val fOtherwise =
          f(s.otherwise)

        F.map2(fMap, fOtherwise)(FieldReqTypeRulesRO(_, _))
      }
    }

  def const[D](res: Resolution[D]): FieldReqTypeRulesRO[D] =
    FieldReqTypeRulesRO(Map.empty, res)

  def defaultTo[D](d: D) = const(Resolution.DefaultTo(d))
  def notApplicable      = const(Resolution.NotApplicable)

  def only[D](reqTypeId: ReqTypeId, resolution: Resolution[D]): FieldReqTypeRulesRO[D] =
    FieldReqTypeRulesRO(Map(reqTypeId -> resolution), Resolution.NotApplicable)

  // type ForTextField = FieldReqTypeRulesRO[Impossible]

  sealed abstract class Resolution[+Default](final val applicability: Applicability) {
    final def isNA = applicability is NA
    def isDefault = false
    def defaultOption: Option[Default] = None
  }

  object Resolution {

    case object NotApplicable extends Resolution[Nothing](NA)

    final case class DefaultTo[+D](default: D) extends Resolution[D](Applicable) {
      override def isDefault = true
      override val defaultOption = Some(default)
    }

    // type ForTextField = Resolution[Impossible]
  }

  final case class ByResolution[D](perRes: Map[Resolution[D], NonEmptySet[ReqTypeId]], otherwise: Resolution[D]) {

    def filterReqTypeIds(f: ReqTypeId => Boolean): ByResolution[D] = {
      val updated =
        perRes.iterator.flatMap { case (res, ids) =>
          NonEmptySet.option(ids.whole.filter(f)).iterator.map((res, _))
        }.toMap
      copy(perRes = updated)
    }

    def filterLiveReqTypes(r: ReqTypes): ByResolution[D] =
      filterReqTypeIds(r.live(_, Dead) is Live)

    lazy val toRules: FieldReqTypeRulesRO[D] = {
      val byId =
        perRes.iterator.flatMap { case (res, ids) =>
          ids.iterator.map((_, res))
        }.toMap
      FieldReqTypeRulesRO(byId, otherwise)
    }
  }

  object ByResolution {
    def build[D](perRes: IterableOnce[(Resolution[D], Set[ReqTypeId])], otherwise: Resolution[D]): ByResolution[D] = {
      var m = Map.empty[Resolution[D], NonEmptySet[ReqTypeId]]
      for {
        (res, ids) <- perRes.iterator
        id         <- ids
      }
        m = m.setOrModifyValue(res, NonEmptySet one id, _ + id)

      m -= otherwise
      apply(m, otherwise)
    }
  }

  implicit def univEqResolution         [D: UnivEq]: UnivEq[Resolution         [D]] = UnivEq.derive
  implicit def univEqByResolution       [D: UnivEq]: UnivEq[ByResolution       [D]] = UnivEq.derive
  implicit def univEqFieldReqTypeRulesRO[D: UnivEq]: UnivEq[FieldReqTypeRulesRO[D]] = UnivEq.derive
}
