package shipreq.webapp.member.jsfacade

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.js.|

@JSGlobal("Y")
@js.native
@nowarn
object Yjs extends js.Object {

  def applyUpdate(doc: Doc, update: Update): Unit = js.native

  /**
   * @param targetStateVector The state of the target that receives the update. Leave empty to write all known structs
   */
  def encodeStateAsUpdate(doc: Doc, targetStateVector: StateVector = js.native): Update = js.native

  def encodeStateVector(doc: Doc): StateVector = js.native

//  def snapshot(doc: Doc): Snapshot = js.native

  // ===================================================================================================================

  type Delta       = js.Array[js.Object]
  type Origin      = js.Any
  type Update      = Uint8Array
  type StateVector = Uint8Array

  type Value =
    Uint8Array |
    AbstractType |
    String | JsNumber | Boolean // JSON - currently excluding object & null

  @js.native
  final class ID(val client: Int, val clock: Int) extends js.Object

  @js.native
  final class Transaction extends js.Object

  @js.native
  final class Snapshot extends js.Object

  @js.native
  final class Doc extends js.Object {
    var guid    : String = js.native
    var clientID: Int    = js.native
    val store   : Store  = js.native

    def getArray(name: String = js.native): YArray = js.native
    def getMap  (name: String = js.native): YMap   = js.native
    def getText (name: String = js.native): YText  = js.native

    /**
     * @param origin Transient data passed to any event listeners.
     */
    def transact(f: js.Function1[Transaction, Unit], origin: Origin = js.native): Unit = js.native
  }

  @js.native
  sealed abstract class AbstractType extends js.Object {
    val doc: Doc = js.native
  }

  @js.native
  final class YArray extends AbstractType {
    def length: Int = js.native
    def toArray(): js.Array[Value] = js.native
    def insert(index: Int, content: js.Array[Value]): Unit = js.native
    def delete(index: Int, length: Int): Unit = js.native
    def push(content: js.Array[Value]): Unit = js.native
    def get(index: Int): js.UndefOr[Value] = js.native
    def slice(start: Int, end: Int = js.native): js.Array[Value] = js.native
  }

  @js.native
  final class YMap extends AbstractType {
     def set(key: String, value: Value): Unit = js.native
     def get(key: String): js.UndefOr[Value] = js.native
     def delete(key: String): Unit = js.native
     def has(key: String): Boolean = js.native
     def entries(): js.Iterator[js.Array[js.Any]] = js.native
     def values(): js.Iterator[Value] = js.native
     def keys(): js.Iterator[String] = js.native
  }

  @js.native
  final class YText extends AbstractType {
    def length: Int = js.native
    @JSName("toString") def strValue(): String = js.native
    def insert(index: Int, content: String): Unit = js.native
    def delete(index: Int, length: Int): Unit = js.native
    def toDelta(snapshot: Snapshot = js.native, prevSnapshot: Snapshot = js.native): Delta = js.native
    def applyDelta(d: Delta): Unit = js.native
  }

  @js.native
  final class Store extends js.Object {
    val clients: js.Map[Int, js.Array[Item]] = js.native
  }

  @js.native
  final class Item extends js.Object {
    val id         : ID                       = js.native
    val left       : Item | Null              = js.native
    val origin     : ID | Null                = js.native
    val right      : Item | Null              = js.native
    val rightOrigin: ID | Null                = js.native
    val parent     : AbstractType | ID | Null = js.native
    val parentSub  : String | Null            = js.native
  //val content    : AbstractContent          = js.native

    var marker : Boolean = js.native
    var keep   : Boolean = js.native
    var deleted: Boolean = js.native

    /** Returns the next non-deleted item */
    def next(): Item | Null = js.native

    /** Returns the previous non-deleted item */
    def prev(): Item | Null = js.native

    /** Computes the last content address of this Item. */
    def lastId(): ID = js.native

    def markDeleted(): Unit = js.native

    def delete(transaction: Transaction): Unit = js.native
  }
}
