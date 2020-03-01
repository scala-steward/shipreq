package shipreq.webapp.base.feature

import japgolly.scalajs.react.{Callback, CallbackTo}
import japgolly.scalajs.react.vdom.TagMod

/** Allows a user to drag items in a sequence to reorder them.
  *
  * Usage:
  *
  * 1. In your backend, call `DragToReorderFeature.apply` and store the result as a private val.
  * 2. In your render function...
  *   1. mix `.container` into the parent vdom (i.e. the droppable container)
  *   2. call `.items` and render each item as desired making sure to...
  *     1. Either mix `.mod` into the list item,
 *         or mix `.source` into the drag handle and `.target` into the drop area
  *     2. style the list item according to `.status`
  */
object DragToReorderFeature {
  import dragtoreorder.Instance

  /**
   * @param updateUI Use `$.forceUpdate`
   */
  def apply[A](getData            : CallbackTo[Vector[A]],
               updateData         : Vector[A] => Callback,
               updateUI           : Callback,
               dragOutsideToRemove: Boolean,
               addKeysToChildren  : Boolean = true,
              ): DragToReorderFeature[A] =
    new Instance(
      getData             = getData,
      updateData          = updateData,
      updateUI            = updateUI,
      dragOutsideToRemove = dragOutsideToRemove,
      addKeysToChildren   = addKeysToChildren,
    )

  type Status = dragtoreorder.Status
  val  Status = dragtoreorder.Status

  final case class Item[+A](data: A, source: TagMod, target: TagMod, status: Status) {
    def mod = TagMod(source, target)
  }
}

trait DragToReorderFeature[A] {

  val container: TagMod

  /** Only call this in your render function. It's unsafe (impure) otherwise. */
  def items(): Vector[DragToReorderFeature.Item[A]]

  def items(as: IndexedSeq[A]): Vector[DragToReorderFeature.Item[A]]
}
