package shipreq.webapp.client.app.ui

import japgolly.scalajs.react._, vdom.prefix_<^._
import japgolly.scalajs.react.extra._
import shipreq.base.util.UnivEq
import shipreq.webapp.client.lib.ui.UI
import shipreq.webapp.client.util.{Off, On}

import Selection._

/**
 * Data selected by the user.
 */
final class Selection[A] private[Selection](val selected: Set[A]) {
  override def toString = s"Selection($selected)"

  def updateBy(f: UpdateFn[A]): WithUpdateFn[A] =
    new WithUpdateFn(selected, f)

  def updateByNoReuse(f: Selection[A] => Callback): WithUpdateFn[A] =
    updateBy(ReusableFn(f))
}

object Selection {
  def apply[A](selected: Set[A]): Selection[A] =
    new Selection(selected)

  def empty[A: UnivEq]: Selection[A] =
    Selection(UnivEq.emptySet)

  type UpdateFn[A] = Selection[A] ~=> Callback

  final class WithUpdateFn[A] private[Selection](val selected: Set[A], val updateFn: UpdateFn[A]) {
    override def toString = s"Selection($selected)"

    def apply(a: A) =
      new OneUI(a, selected, updateFn)

    def visible(visible: Set[A]) =
      new VisibleWithUpdateFn(selected, visible, updateFn)
  }

  final class VisibleWithUpdateFn[A] private[Selection](val selected: Set[A], val visible: Set[A], val updateFn: UpdateFn[A]) {
    override def toString = s"Selection.Visible(\n  selected: $selected,\n  visible: $visible)"

    val (visibleSelection, hiddenSelection) =
      selected partition visible.contains

    def apply(a: A) =
      new OneUI(a, selected, updateFn)

    val total = new TotalUI(visible, visibleSelection, hiddenSelection, updateFn)
  }

  sealed trait Focus[A, Get] {
    val get: Get
    def set(n: On): Selection[A]
    def toggle: Selection[A]
  }

  sealed trait UI[A, Get, Checkbox] extends Focus[A, Get] {
    val updateFn: UpdateFn[A]
    def toggleFn: Callback = updateFn(toggle)
    def checkbox: Checkbox
    def checkboxAndOnClick: TagMod

    final def onClick: TagMod =
      TagMod (^.onClick --> toggleFn, ^.cursor.pointer)
  }

  final class OneUI[A](a: A, selected: Set[A], override val updateFn: UpdateFn[A]) extends UI[A, On, ReactTag] {
    override val get =
      On <~ selected.contains(a)

    override def set(newState: On) =
      newState match {
        case On  => Selection(selected + a)
        case Off => Selection(selected - a)
      }

    override def toggle =
      set(!get)

    override def checkbox =
      UI.checkbox(get)(^.onChange --> toggleFn)

    override def checkboxAndOnClick: TagMod =
      TagMod(checkbox, onClick)
  }

  final class TotalUI[A](visible: Set[A], visibleSelection: Set[A], hiddenSelection: Set[A],
                         override val updateFn: UpdateFn[A]) extends UI[A, Option[On], ReactElement] {
    override val get =
      if (visible.isEmpty)
        None
      else if (visibleSelection.isEmpty)
        Some(Off)
      else if (visibleSelection.size == visible.size)
        Some(On)
      else
        None

    override def set(newState: On): Selection[A] =
      newState match {
        case On  => Selection(hiddenSelection | visible)
        case Off => Selection(hiddenSelection)
      }

    override def toggle: Selection[A] =
      set(Checkbox3 nextState get)

    override def checkbox: ReactElement =
      Checkbox3.Component(Checkbox3.Props(get, updateFn compose set))

    override def checkboxAndOnClick: TagMod =
      TagMod(checkbox, onClick)
  }


  implicit def reuseSel[A]: Reusability[Selection[A]]           = Reusability.byRef || Reusability.by(_.selected)
  implicit def reuseVis[A]: Reusability[VisibleWithUpdateFn[A]] = Reusability.byRef || Reusability.by(v => (v.selected, v.visible, v.updateFn))
}
