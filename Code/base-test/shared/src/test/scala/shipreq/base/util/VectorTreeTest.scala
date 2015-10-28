package shipreq.base.util

import nyaya.gen._
import nyaya.prop._
import nyaya.test.PropTest._
import utest._
import shipreq.base.test.BaseUtilGen._
import UnivEq.Implicits._
import VectorTree.{apply => _, _}

object VectorTreeTest extends TestSuite {

  val genIntTree = genVectorTree(Gen.int, 4)

  def allNodes[A](n: Node[A]): Vector[A] =
    n.children.flatMap(allNodes) :+ n.value

  override def tests = TestSuite {

    'values {
      val p = Prop.equal[VectorTree[Int]]("values")(
        _.valueIterator.toVector.sorted,
        _.children.flatMap(allNodes).sorted)
      p mustBeSatisfiedBy genIntTree
    }

    'locAndValueIterator {
      'values {
        val p = Prop.equal[VectorTree[Int]]("iterateWithLoc.values")(
          t => t.locAndValueIterator((_, i) => i).toVector.sorted,
          _.children.flatMap(allNodes).sorted)
        p mustBeSatisfiedBy genIntTree
      }
      'locations {
        val p = Prop.atom[VectorTree[Int]]("iterateWithLoc.locs", t => {
          val results = t.locAndValueIterator((_, _))
          val bad = results.filter{ case(l, i) => t.getAtLocation(l) != Option(i) }.toList
          bad.headOption.map{ case(l, i) => s"Bad result: (${l.whole mkString "."}) = ${t.getAtLocation(l)} not $i}" }
        })
        p mustBeSatisfiedBy genIntTree
      }
    }

  }
}
