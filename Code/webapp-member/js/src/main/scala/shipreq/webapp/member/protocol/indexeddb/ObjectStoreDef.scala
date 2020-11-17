package shipreq.webapp.member.protocol.indexeddb

import japgolly.scalajs.react.{AsyncCallback, CallbackTo}
import scala.scalajs.js

sealed trait ObjectStoreDef[V] {
  val name: String
}

object ObjectStoreDef {

  final case class Sync[V](name: String, codec: ValueCodec[V]) extends ObjectStoreDef[V]

  final case class Async[V](name: String, codec: ValueCodec.Async[V]) extends ObjectStoreDef[V] { self =>

    type Value = Async.Value {
      type ValueType = V
      val store: self.type
    }

    def encode(v: V): AsyncCallback[Value] =
      codec.encode(v).map(value)

    def value(v: js.Any): Value =
      new Async.Value {
        override type ValueType = V
        override val store: self.type = self
        override val value = v
      }

    val sync: Sync[Value] = {
      val codec = ValueCodec[Value](
        encode = v => CallbackTo.pure(v.value),
        decode = v => CallbackTo.pure(value(v)),
      )
      Sync(name, codec)
    }
  }

  object Async {

    sealed trait Value {
      type ValueType
      val store: Async[ValueType]
      val value: js.Any

      final def decode: AsyncCallback[ValueType] =
        store.codec.decode(value)
    }

  }

}