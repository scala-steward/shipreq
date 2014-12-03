package shipreq.webapp.client.util.ui.tablespec2

import scalaz.Equal
import scalaz.effect.IO
import shipreq.prop.test.Gen
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
}