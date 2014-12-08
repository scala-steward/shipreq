package shipreq.webapp.client.util.ui.tablespec2

import scalaz.Equal
import scalaz.std.anyVal.longInstance
import scalaz.effect.IO
import shipreq.prop.test.Gen
import shipreq.base.util.ScalaExt._
import shipreq.webapp.base.TextMod._
import shipreq.webapp.base.validation2.Constraints._
import shipreq.webapp.base.validation2._
import RowStatus._

object TestUtil {

  case class AB[A,B](a: A, b: B)

  def genAB[A, B](ga: Gen[A], gb: Gen[B]): Gen[AB[A,B]] =
    Gen.apply2(AB.apply[A, B])(ga, gb)

  type TestFields2[A, B] = FieldSet2[AB[A, B], A, B]

  def fields2[A,B](empty: (A,B)): TestFields2[A, B] =
    FieldSet2[AB[A,B]](_.a, _.b)(empty)

  implicit val eqRowStatus = Equal.equalA[RowStatus]

  val failedRowStatus =
    Failed(IO(()))

  def genRowStatus: Gen[RowStatus] =
    Gen.oneof(Sync, Locked, failedRowStatus)

  object SampleData_Person {

    case class Username(value: String)
    object Username {
      implicit val equal = Equal.equalA[Username]
    }
    case class Person(id: Long, username: Username, desc: Option[String])

    type VS = (Stream[Person], Option[Long])

    val usernameF = "Username"

    val usernameVU = Validator(
      CorrectionPart
        .endo(noWhitespace andThen lowerCase)
        .addLiveCorrect(_.toLowerCase),
      ValidationPart.forConstraint(usernameF,
        lengthInRange(2 to 16)
          + whitelistCharsR("a-z0-9_")("can only contain letters, numbers and underscores.")
          + startsWithR("[a-z]")("must start with a letter.")
          + endsWithR("[a-z0-9]")("must end with a letter or a number.")
      )) map Username.apply

    val uniqueUsername = Uniqueness.entity[Person].applyO(_.id.some, _.username).fieldName(usernameF)

    val usernameV = usernameVU.liftS[VS].addValidation(uniqueUsername)

    val descVU = GenericValidators.optionalLargeText("Desc")
    val descV = descVU.liftS[VS]

    val personV = usernameV ⊗ descV

    val fields = FieldSet2[Person](_.username.value, _.desc getOrElse "")(("", "TODO"))
  }
}