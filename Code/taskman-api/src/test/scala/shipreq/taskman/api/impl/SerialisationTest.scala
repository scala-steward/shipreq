package shipreq.taskman.api.impl

import japgolly.microlibs.testutil.TestUtil._
import scalaz.\/-
import scalaz.std.string._
import utest._
import shipreq.base.test.MTestScalaCheck
import shipreq.base.util.TaggedTypes.JsonStr
import shipreq.taskman.api.TestHelpers._
import shipreq.taskman.api.impl.Serialisation._
import shipreq.taskman.api.{EmailAddr, Msg, MsgType, UserId}

object SerialisationTest extends TestSuite with MTestScalaCheck {

  implicit def jstr(s: String): Ser = JsonStr[Msg](s)

  override def tests = Tests {

    "serialise and deserialise back" - scalaCheck(_.forAll((m: Msg) =>
      deserialise(MsgType.lookup(m).id, serialise(m)) == \/-(m)
    ))

    "serialise Msg.RegistrationCompleted" - {
      val m = Msg.RegistrationCompleted(UserId(666))
      assertEq(serialise(m).value, """{"u":666}""")
    }

    "serialise Msg.ReRegistrationAttempted" - {
      val m = Msg.ReRegistrationAttempted(EmailAddr("x@x.com"))
      assertEq(serialise(m).value, """{"e":"x@x.com"}""")
    }

    "return an error for unknown task types" - {
      val r = deserialise(-500.toShort, "")
      assert(r.toEither.left.exists(_.toString  contains "-500"))
    }

    "return an error if data fails parsing" - {
      val r = deserialise(MsgType.RegistrationRequested.id, """{"x":1}""")
      assert(r.isLeft)
    }

  }
}
