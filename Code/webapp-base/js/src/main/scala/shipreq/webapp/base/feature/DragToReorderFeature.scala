package shipreq.webapp.base.feature

import japgolly.scalajs.react.{Callback, CallbackTo}
import japgolly.scalajs.react.vdom.TagMod

/**
 * Usage:
 *
 * 1. In your backend, call `DragToReorderFeature(_).prepare(_)` and store the result as a private val.
 * 2. In your render function...
 *   1. mix `.container` into the parent vdom (i.e. the droppable container)
 *   2. call `.items()` and render each item as desired making sure to...
 *     1. mix `.mod` into the list item
 *     2. style the list item according to `.status`
 *
 */
object DragToReorderFeature {
  import dragtoreorder.Instance

  /**
   * @param updateUI Use `$.forceUpdate`
   */
  def apply[A](updateUI: Callback, updateOrder: Vector[A] => Callback): DragToReorderFeature[A] =
    new Instance(updateUI, updateOrder)

  type Status = dragtoreorder.Status
  val  Status = dragtoreorder.Status

  final case class Item[+A](data: A, mod: TagMod, status: Status)

  trait Prepared[A] {

    /** Same as DragToReorderFeature.container here for convenience. */
    val container: TagMod

    /** Only call this in your render function. It's unsafe (impure) otherwise. */
    def items(): Vector[Item[A]]
  }
}

trait DragToReorderFeature[A] {

  val container: TagMod

  def prepare(data: CallbackTo[Vector[A]]): DragToReorderFeature.Prepared[A]
}
