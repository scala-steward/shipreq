package shipreq.webapp.base.hash2

import scalaz.Equal
import utest._
import shipreq.base.test.BaseTestUtil._

object Hash2Tests extends TestSuite {


  // TODO associativity

  def consPropAss[A, B: Equal](inputAs: Vector[A])(hashRecs: A => HashRec.Collection, ab: A => B): Unit = {
    val results = HashLogic.consolidate(inputAs)(hashRecs, ab)

  def consProp1[A, B: Equal](inputAs: Vector[A])(hashRecs: A => HashRec.Collection, ab: A => B): Unit = {
    val results = HashLogic.consolidate(inputAs)(hashRecs, ab)

    val inputBs = inputAs.map(ab)
    assertEq(inputBs, results.flatMap(_._1).toVector)

    // latest exists as is
    for (lastHRsI <- inputAs.lastOption.map(hashRecs)) {
      val lastHRsO = results.last._2.toMap
      for (hr <- lastHRsI)
        assertEq(lastHRsO.get(hr.scheme).flatMap(_.get(hr.scope)), Some(hr.hash))
    }

    // All scopes/scheme are populated
    for ((scheme, scopes) <- results.flatMap(_._2))
      assertEq(scopes.scopeIterator.toSet, scheme.hashFns.scopeIterator.toSet)

    ???
  }

  // Ev #1 -
  // Ev #2 -

  override def tests = TestSuite {

    'consolidate {

    }
  }
}
