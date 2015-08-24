package shipreq.webapp.client.util

import japgolly.scalajs.react.{Callback, CallbackTo, ReactKeyboardEventH}
import org.scalajs.dom._
import DomPatches._

object DomUtil {

  @inline implicit class PatchNode(private val n: Node) extends AnyVal {
    @inline def castDom[T <: Node] = n.asInstanceOf[T]
    @inline def castHtml = castDom[html.Element]
  }

  def checkModKeys(e       : ReactKeyboardEventH,
                   altKey  : Boolean = false,
                   ctrlKey : Boolean = false,
                   metaKey : Boolean = false,
                   shiftKey: Boolean = false): Boolean =
    e.altKey   == altKey   &&
    e.ctrlKey  == ctrlKey  &&
    e.metaKey  == metaKey  &&
    e.shiftKey == shiftKey

  /**
   * Determine the index of an element amongst its parent's children.
   *
   * @return ≥ 0
   */
  def siblingIndex(e: html.Element): CallbackTo[Int] =
    CallbackTo {
      var m: Option[Element] = Some(e)
      def next() = {
        m = Option(m.get.previousElementSibling)
        m.isDefined
      }
      var i = 0
      while (next())
        i += 1
      i
    }

  def siblingAtOffset(e: html.Element, offset: Int): CallbackTo[Element] =
    siblingIndex(e).map { cur =>
      val sibs = e.parentElement.children
      val max = sibs.length
      var i = (cur + offset) % max
      if (i < 0)
        i += max
      sibs(i)
    }
}
