package shipreq.webapp.base.protocol2

import boopickle.{PickleImpl, Pickler, UnpickleImpl}
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array, TypedArrayBuffer}
import scala.scalajs.js.typedarray.TypedArrayBufferOps._

object BinaryJs {

  def encode[A: Pickler](a: A): Int8Array = {
    val bb = PickleImpl.intoBytes(a)
    bb.typedArray().subarray(0, bb.limit)
  }

  def decode[A: Pickler](ab: ArrayBuffer): A = {
    val bb = TypedArrayBuffer.wrap(ab)
    UnpickleImpl[A].fromBytes(bb)
  }

  def decodeUnsafe[A: Pickler](a: Any): A =
    decode[A](a.asInstanceOf[ArrayBuffer])

}
