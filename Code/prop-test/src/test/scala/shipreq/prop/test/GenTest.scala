package shipreq.prop.test

import scalaz.std.list._
import utest._
import shipreq.prop._
import TestUtil._

object GenTest extends TestSuite {

  val prop = Prop[List[Int]]("distinct ints", is => is.distinct == is)
  val intGen = Gen.chooseint(0,5).list.lim(10).map(Distinct.int.lift[List].run)

  override def tests = TestSuite {

    'distinct {
      prop mustBeSatisfiedBy intGen
    }
  }
}
