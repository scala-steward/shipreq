package shipreq.webapp.client.util

import org.scalajs.dom._
import scala.scalajs.js

object DomPatches {

  // TODO Remove when scala-js-dom 0.8.2 released

  /**
   * The NonDocumentTypeChildNode interface contains methods that are particular to
   * Node objects that can have a parent, but not suitable for DocumentType.
   *
   * NonDocumentTypeChildNode is a raw interface and no object of this type can be
   * created; it is implemented by Element, and CharacterData objects.
   *
   * https://developer.mozilla.org/en-US/docs/Web/API/NonDocumentTypeChildNode
   */
  trait NonDocumentTypeChildNode extends js.Object {

    /**
     * The previousElementSibling read-only property returns the Element immediately prior
     * to the specified one in its parent's children list, or null if the specified element
     * is the first one in the list.
     *
     * MDN
     */
    def previousElementSibling: Element = js.native

    /**
     * The nextElementSibling read-only property returns the element immediately following
     * the specified one in its parent's children list, or null if the specified element is
     * the last one in the list.
     *
     * MDN
     */
    def nextElementSibling: Element = js.native
  }

  @inline implicit def patchElementToNonDocumentTypeChildNode(e: Element) =
    e.asInstanceOf[NonDocumentTypeChildNode]

  @inline implicit def patchCharacterDataToNonDocumentTypeChildNode(cd: CharacterData) =
    cd.asInstanceOf[NonDocumentTypeChildNode]
}
