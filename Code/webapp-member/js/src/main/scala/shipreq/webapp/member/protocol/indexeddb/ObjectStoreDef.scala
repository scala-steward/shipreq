package shipreq.webapp.member.protocol.indexeddb

import japgolly.scalajs.react.{AsyncCallback, CallbackTo}
import scala.scalajs.js

sealed trait ObjectStoreDef[K, V] {
  val name: String
  val keyCodec: KeyCodec[K]
}

object ObjectStoreDef {

  final case class Sync[K, V](name      : String,
                              keyCodec  : KeyCodec[K],
                              valueCodec: ValueCodec[V]) extends ObjectStoreDef[K, V]

  final case class Async[K, V](name      : String,
                               keyCodec  : KeyCodec[K],
                               valueCodec: ValueCodec.Async[V]) extends ObjectStoreDef[K, V] { self =>

    type Value = Async.Value {
      type KeyType = K
      type ValueType = V
      val store: self.type
    }

    def encode(v: V): AsyncCallback[Value] =
      valueCodec.encode(v).map(value)

    def value(v: js.Any): Value =
      new Async.Value {
        override type KeyType = K
        override type ValueType = V
        override val store: self.type = self
        override val value = v
      }

    val sync: Sync[K, Value] = {
      val syncValueCodec = ValueCodec[Value](
        encode = v => CallbackTo.pure(v.value),
        decode = v => CallbackTo.pure(value(v)),
      )
      Sync(name, keyCodec, syncValueCodec)
    }
  }

  object Async {

    sealed trait Value {
      type KeyType
      type ValueType
      val store: Async[KeyType, ValueType]
      val value: js.Any

      final def decode: AsyncCallback[ValueType] =
        store.valueCodec.decode(value)
    }

  }

}