package shipreq.base.util.log

import com.fasterxml.jackson.core.JsonGenerator
import io.circe.Encoder
import io.circe.syntax._
import japgolly.univeq._
import java.time.Duration
import net.logstash.logback.argument.StructuredArgument

final class LogField[-A] private[LogField] (val key: String,
                                            val fieldType: LogField.Type,
                                            create: (String, A) => StructuredArgument) {

  LogField.Registry.register(key, fieldType)

  def apply(a: A): StructuredArgument =
    create(key, a)

  private def copy[B](create2: (String, B) => StructuredArgument): LogField[B] =
    new LogField(key, fieldType, create2)

  def contramap[B](f: B => A): LogField[B] =
    copy((k, b) => create(k, f(b)))

  def optional: LogField[Option[A]] =
    copy((k, o) => o.fold(LogField.emptyArg)(create(k, _)))
}

object LogField {

  sealed trait Type

  private def creator[A](write: (JsonGenerator, A) => Unit): (String, A) => StructuredArgument =
    (key, value) =>
      new StructuredArgument {
        override def writeTo(g: JsonGenerator) = {
          g.writeFieldName(key)
          write(g, value)
        }
        override def toString = "" + value
    }

  case object Text extends Type {
    private val create = creator[String](_.writeString(_))

    def apply(key: String): LogField[String] =
      new LogField(key, this, create)

    def json[A: Encoder](key: String): LogField[A] =
      Text(key).contramap(_.asJson.noSpaces)
  }

  case object Long extends Type {
    private val create = creator[Long](_.writeNumber(_))

    def apply(key: String): LogField[Long] =
      new LogField(key, this, create)

    def durationMs(key: String): LogField[Duration] =
      Long(key).contramap { d =>
        try
          d.toMillis
        catch {
          case _: ArithmeticException => scala.Long.MaxValue
        }
      }
  }

  case object Bool extends Type {
    private val create = creator[Boolean](_.writeBoolean(_))

    def apply(key: String): LogField[Boolean] =
      new LogField(key, this, create)
  }

  /** Unsafe because once ElasticSearch sees a certain type at a certain path, it tries to hold all future data to the
    * same expectation. This is safe so long as the JSON encoder always generates the same types at the same paths.
    */
  final case class UnsafeJson[A](encoder: Encoder[A]) extends Type {
    override def equals(obj: Any) = obj match {
      case a: UnsafeJson[_] => encoder eq a.encoder
      case _                => false
    }
  }

  object UnsafeJson {
    def apply[A: Encoder](key: String): LogField[A] =
      new LogField(
        key,
        new UnsafeJson(Encoder[A]),
        (k, a) =>
          new StructuredArgument {
            override def writeTo(g: JsonGenerator): Unit = {
              g.writeFieldName(k)
              g.writeRawValue(a.asJson.noSpaces)
            }
            override def toString = "" + a
        }
      )
  }

  // ===================================================================================================================

  implicit def univEqUnsafeJson_ : UnivEq[UnsafeJson[_]] = UnivEq.force
  implicit def univEqType        : UnivEq[Type]          = UnivEq.derive

  private[LogField] object Registry {
    private val lock  = new AnyRef
    private var state = Map.empty[String, Type]

    def register(k: String, t: Type): Unit =
      lock.synchronized {
        state.get(k) match {
          case Some(t2) =>
            if (t !=* t2)
              throw new ExceptionInInitializerError(s"Attempted to use log field '$k' with varying data types: $t & $t2")
          case None =>
            state = state.updated(k, t)
        }
      }
  }

  // ===================================================================================================================

  val emptyArg: StructuredArgument =
    new StructuredArgument {
      override def writeTo(g: JsonGenerator) = ()
      override def toString                  = ""
    }

}
