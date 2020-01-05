package shipreq.webapp.base.lib

import japgolly.scalajs.react.{Callback, CallbackTo}
import org.scalajs.dom.raw.{Storage => StorageJs}
import scala.scalajs.js

trait BrowserStorage {
  def clear: Callback
  def getItem(key: String): CallbackTo[Option[String]]
  def removeItem(key: String): Callback
  def setItem(key: String, data: String): Callback

  //  def getLength: CallbackTo[Int]
  //  def key(index: Int): CallbackTo[Option[String]]
}

object BrowserStorage {

  // Keep this as a def in case undefined at first, then later user grants access and it becomes available.
  // Is that a valid scenario? I don't know but may as well support it if possible.
  def local: Option[BrowserStorage] = {
    val available = org.scalajs.dom.window.localStorage.asInstanceOf[js.UndefOr[StorageJs]]
    available.toOption.flatMap(Option(_)).map(new Real(_))
  }

  def localOrEmpty: BrowserStorage =
    local.getOrElse(Empty)

  object Empty extends BrowserStorage {
    override def clear: Callback =
      Callback.empty

    override def getItem(key: String): CallbackTo[Option[String]] =
      CallbackTo.pure(None)

    override def removeItem(key: String): Callback =
      Callback.empty

    override def setItem(key: String, data: String): Callback =
      Callback.empty
  }

  private final class Real(storageJs: StorageJs) extends BrowserStorage {
    override def clear: Callback =
      Callback(storageJs.clear())

    override def getItem(key: String): CallbackTo[Option[String]] =
      CallbackTo(Option(storageJs.getItem(key)))

    override def removeItem(key: String): Callback =
      Callback(storageJs.removeItem(key))

    override def setItem(key: String, data: String): Callback =
      Callback(storageJs.setItem(key, data))
  }

  final class InMemory extends BrowserStorage {
    private var state = Map.empty[String, String]

    override def clear: Callback =
      Callback {state = Map.empty}

    override def getItem(key: String): CallbackTo[Option[String]] =
      CallbackTo(state.get(key))

    override def removeItem(key: String): Callback =
      Callback {state -= key}

    override def setItem(key: String, data: String): Callback =
      Callback {state = state.updated(key, data)}
  }

  // ===================================================================================================================

  final class Field[A](key: String, write: A => String, read: String => Option[A]) {

    def get(implicit s: BrowserStorage): CallbackTo[Option[A]] =
      s.getItem(key).map(_.flatMap(read))

    def set(value: A)(implicit s: BrowserStorage): Callback =
      s.setItem(key, write(value))

    def remove(implicit s: BrowserStorage): Callback =
      s.removeItem(key)

    def setOrRemove(value: Option[A])(implicit s: BrowserStorage): Callback =
      value.fold(remove)(set(_))

    def map[B](f: A => Option[B])(g: B => A): Field[B] =
      new Field(key, write compose g, read(_).flatMap(f))

    def xmap[B](f: A => B)(g: B => A): Field[B] =
      map(f.andThen(Some(_)))(g)
  }

  object Field {
    def apply(key: String): Field[String] =
      new Field(key, identity, Some(_))

    def boolean(key: String): Field[Boolean] =
      apply(key).map({
        case "1" => Some(true)
        case "0" => Some(false)
        case _   => None
      })({
        case true  => "1"
        case false => "0"
      })
  }

}
