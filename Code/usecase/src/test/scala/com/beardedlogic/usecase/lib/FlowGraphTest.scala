package com.beardedlogic.usecase.lib

import org.scalatest.FunSpec
import scalaz.{NonEmptyList => NEL}
import com.beardedlogic.usecase.test.TestData
import FlowGraph._
import Types._

class FlowGraphTest extends FunSpec with TestData {

  implicit def s2n(x: String): Node = x.asLabel
  implicit def autoLabelT(t: (String, String)): ExplicitFlow = (t._1.asLabel, t._2.asLabel)
  implicit def autoLabelL(l: List[String]): List[Node] = l map s2n
  def nl(prefix: String, range: Range): List[Node] = range map (i => s"$prefix.$i".asLabel) toList
  //def nnel(prefix: String, range: Range): NEL[Node] = nl(prefix, range) match {case h::t => NEL(h,t:_*)}
  def hnNel(prefix: String, range: Range): NEL[Node] = NEL(prefix, nl(prefix, range): _*)

  import Renderer.{render, NC, AC, EC}

  describe("Modeller") {
    import Modeller._

    lazy val m = model(MockUc4.UC)

    it("startNodes") {
      m.socialData.startNodes.sorted ==== List("7.0", "7.2")
    }
    it("endNodes") {
      m.socialData.endNodes.sorted ==== List("7.0.3", "7.1")
    }
    it("explicitFlows") {
      m.socialData.explicitFlows.sorted ==== List(("7.0.2.a" -> "7.0.1"), ("7.0.2.a" -> "7.1"), ("7.2.1" -> "7.0.3"))
    }
    it("NC.headNodes") {
      m.intraCatData(NC).headNodes ==== List("7.0")
    }
    it("NC.implicitFlows") {
      m.intraCatData(NC).implicitFlows ==== List(NEL("7.0", "7.0.1", "7.0.2", "7.0.2.a", "7.0.3"))
    }
    it("AC.headNodes") {
      m.intraCatData(AC).headNodes ==== List("7.1", "7.2")
    }
    it("AC.implicitFlows") {
      m.intraCatData(AC).implicitFlows ==== List(NEL("7.1"), hnNel("7.2", 1 to 1))
    }
  }

  describe("Renderer") {
    val nc = IntraCatData(List("1.0"), List(hnNel("1.0", 1 to 9)))
    val ac = IntraCatData(List("1.1"), List(hnNel("1.1", 1 to 5)))
    val ec = IntraCatData(List("1.E.1"), List(hnNel("1.E.1", 1 to 3)))
    val ef: List[ExplicitFlow] = List(
      ("1.0.4" -> "1.0.7")
      , ("1.0.6" -> "1.0.1")
      , ("1.0.1" -> "1.1")
      , ("1.0.4" -> "1.1")
      , ("1.1.5" -> "1.0.1")
      , ("1.0.9" -> "1.E.1")
      , ("1.E.1.3" -> "1.0.1")
    )
    val sd = SocialData(ef, List("1.0","1.99"), List("1.0.9","99.99"))
    val m = FlowGraphModel(Map(NC -> nc, AC -> ac, EC -> ec), sd)
    lazy val g = render(m).toString

    def expect(s: String): Unit = g should include(s.replace('|','"'))

    it("startNodes") {
      expect("|1.0|;|1.99|")
    }
    it("endNodes") {
      expect("|1.0.9|;|99.99|")
    }
    it("explicitFlows") {
      expect("|1.0.4|->|1.0.7|")
      expect("|1.0.6|->|1.0.1|")
      expect("|1.E.1.3|->|1.0.1|")
    }
    // it("NC.headNodes") {
    it("NC.implicitFlows") {
      expect("|1.0|->|1.0.1|->|1.0.2|->|1.0.3|->|1.0.4|")
    }
    // it("AC.headNodes") {
    it("AC.implicitFlows") {
      expect("|1.1|->|1.1.1|->|1.1.2|->|1.1.3|->|1.1.4|")
    }
    it("EC.implicitFlows") {
      expect("|1.E.1|->|1.E.1.1|->|1.E.1.2|->|1.E.1.3|")
    }
  }

  // it("Generates DOT for arbitrary UCs") {
  // import com.beardedlogic.usecase.test.DataGenerators.useCaseGen
  // val x = useCaseGen(FieldListRec(FL.map(_.rec))).sample.get
  // println(render(model(x)))

  // val sfv = NcSfv
  // val b = DeepBuilder(sfv.textmap, UC.stepsAndLabels.get.ab)
  // val dz = b.build(sfv.tree.head, Nil)
  // implicit val fzs = flattenTopNodes(dz)
  // }
}
