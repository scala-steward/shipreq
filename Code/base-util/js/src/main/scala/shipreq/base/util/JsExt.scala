package shipreq.base.util

import scala.scalajs.js

object JsExt {

  implicit class JsArrayExt[A](private val self: js.Array[A]) extends AnyVal {
    def forEachJs(f: js.Function1[A, Unit]): Unit = {
      self.asInstanceOf[js.Dynamic].forEach(f)
      ()
    }
  }


}
