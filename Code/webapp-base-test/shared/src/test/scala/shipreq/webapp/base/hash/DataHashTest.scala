package shipreq.webapp.base.hash

import upickle._
import upickle.Fns._
import japgolly.nyaya._
import japgolly.nyaya.util.NyayaUtilAnyExt
import japgolly.nyaya.test._
import japgolly.nyaya.test.PropTestOps._
import utest._

import shipreq.webapp.base.test.BaseTestUtil.assertEq
import shipreq.base.util.UnivEq.int
import shipreq.webapp.base.RandomData
import shipreq.webapp.base.data._
import shipreq.webapp.base.protocol.DataCodecs._
import Hash.HashableValueOps
import HashScheme.default._

object DataHashTest extends TestSuite {

  implicit val settings = DefaultSettings.propSettings
    .setSampleSize(80 `JVM|JS` 40).setGenSize(20)
//    .setDebug

  case class HashTest[A: Hash : Reader : Writer](a1: A, a2: A, a3: A) {
    val E = EvalOver(this)

    val h1 = a1.hash
    val h2 = a2.hash
    val h3 = a3.hash

    def consistent(h: Int, a: A) =
      E.equal("consistent: hash(a) = hash(deser(ser(a)))", h, readJs[A](writeJs(a)).hash)

    def allConsistent =
      consistent(h1, a1) ∧ consistent(h2, a2) ∧ consistent(h3, a3)

    def hashesDiffer =
      E.test("Hashes must differ", Set(h1, h2, h3).size >= 2)

    def main =
      allConsistent ∧ hashesDiffer
  }

  def hashTest[A] = Prop.eval[HashTest[A]](_.main)

  def test[A: Hash : Reader : Writer](g: Gen[A]): Unit = {
    val t = for {a <- g; b <- g; c <- g} yield new HashTest(a, b, c)
    hashTest[A] mustBeSatisfiedBy t
  }

  // Ensure that JVM & JS produce the same results
  def testAlgo(a: Hash.Algorithm, expect: AlgorithmResults): Unit = {
    val results = AlgorithmResults.calc(a)
//    println(results)
    assertEq(a.toString, results, expect)
  }

  val murmur3 = AlgorithmResults(1231,1237,1000,-1294967296,1842429670,381277126,-914897307,-1390910323,586134407,2075563892,-1936667874,-1122530123)

  override def tests = TestSuite {
    'murmur3 - testAlgo(MurmurHash3, murmur3)
    'project - test(RandomData.project)
  }
}
