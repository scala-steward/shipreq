package shipreq.base.util

import java.nio.ByteBuffer
import org.scalajs.dom.raw.{Blob, FileReader}
import org.scalajs.dom.window
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array, TypedArrayBuffer}

object BinaryJs extends BinaryJs

trait BinaryJs {
  import JsExt._

  final def arrayBufferToBlob(a: ArrayBuffer): Blob =
    new Blob(js.Array(a))

  @inline final def arrayBufferToByteBuffer(a: ArrayBuffer): ByteBuffer =
    TypedArrayBuffer.wrap(a)

  final def base64ToByteBuffer(base64: String): ByteBuffer = {
    val binstr = window.atob(base64)
    val buf = new Int8Array(binstr.length)
    var i = 0
    binstr.foreach { ch =>
      buf(i) = ch.toByte
      i += 1
    }
    TypedArrayBuffer.wrap(buf)
  }

  final def blobToArrayBuffer(blob: Blob): ArrayBuffer = {
    var arrayBuffer: ArrayBuffer = null
    val fileReader = new FileReader()
    fileReader.onload = e => arrayBuffer = e.target.asInstanceOf[js.Dynamic].result.asInstanceOf[ArrayBuffer]
    fileReader.readAsArrayBuffer(blob)
    assert(arrayBuffer != null)
    arrayBuffer
  }

  final def byteBufferToArrayBuffer(bb: ByteBuffer): ArrayBuffer =
    // TODO hmmm? ByteBuffer -> Int8Array -> ArrayBuffer
    int8ArrayToArrayBuffer(byteBufferToInt8Array(bb))

  final def byteBufferToBlob(bb: ByteBuffer): Blob =
    arrayBufferToBlob(byteBufferToArrayBuffer(bb))

  final def byteBufferToInt8Array(bb: ByteBuffer): Int8Array = {
    if (bb.hasArray)
      bb.typedArray().subarray(0, bb.limit)
    else {
      val array = BinaryData.unsafeFromByteBuffer(bb).unsafeJsArray
      new Int8Array(array)
    }
  }

  final def int8ArrayToArrayBuffer(a: Int8Array): ArrayBuffer =
    a.buffer.slice(0, a.length)

}
