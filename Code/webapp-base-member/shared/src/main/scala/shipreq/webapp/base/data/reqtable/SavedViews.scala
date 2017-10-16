package shipreq.webapp.base.data.reqtable

import japgolly.microlibs.nonempty.NonEmptyVector
import japgolly.univeq.UnivEq
import shipreq.webapp.base.data.FilterDead
import shipreq.webapp.base.filter.ValidFilter
import shipreq.webapp.base.validation.{CommonValidation => V, _}
import shipreq.webapp.base.validation.Simple._
import shipreq.webapp.base.validation.Implicits._

/** A saved configuration of the ReqTable view.
  *
  * It's important to note that once the view is saved, it isn't automatically updated as the project itself changes.
  * Therefore before being applied as a view, it must be reconciled to the current state of the project.
  * For example, one of the columns visible in the saved view could be later deleted, in which case it would have to
  * be ignored when the saved view is used henceforth.
  */
final case class SavedView(name        : SavedView.Name,
                           filterDead  : FilterDead,
                           columns     : NonEmptyVector[Column],
                           sortCriteria: SortCriteria,
                           filter      : Option[ValidFilter]) // TODO Should more of the original text be preserved? (text quotes, Min2Vector instead of Set)

object SavedView {

  final case class Name(value: String) extends AnyVal

  object Name {
    implicit def univEq: UnivEq[Name] = UnivEq.derive

    final val lengthRange = 1 to 40

    val validator: Validator[String, String, Name] =
      V.endoCorrector.singleLineWhitespace
        .withInvalidator(V.invalidator.lengthInRange(lengthRange) merge V.invalidator.containsAlpha)
        .mapInvalidator(V.invalidator.nonEmpty.whenValid)
        .toValidator
        .mapValid(apply)
  }

  implicit def univEq: UnivEq[SavedView] = UnivEq.derive
}

// █████████████████████████████████████████████████████████████████████████████████████████████████████████████████████

object SavedViews {

  type Optional = Option[NonEmpty]

  def empty: Optional = None

  final case class NonEmpty(default: SavedView, nonDefault: Vector[SavedView])

  implicit def univEqNonEmpty: UnivEq[NonEmpty] = UnivEq.derive

}