package com.beardedlogic.usecase
package snippet.uce

import java.util.regex.Pattern
import scala.util.matching.Regex
import org.scalatest.FunSpec
import lib.Types._
import lib.UseCase
import Renderer.TitleId
import test.{LoadedTestData, TestHelpers, TestDatabaseSupport}

class UseCaseEditorTest extends FunSpec with TestDatabaseSupport with TestHelpers with LoadedTestData {

  implicit class StringExt(val x: String) {
    def pp(): String = {println(x); x}
  }

  def unquoteJs(in: String): String =
    "\\\\u([0-9a-f]{4})".r.replaceAllIn(in, m => Integer.parseInt(m.group(1), 16).toChar.toString).replace("\\\"","\"")

  class UseCaseEditor2 extends UseCaseEditor {
    def setState2(newState: State) = { super.setState(newState); this }
    def update2(f: UseCase => UcUpdateResult): (UseCaseEditor2, String) =
      (this, inMockSession {unquoteJs(update(f).toJsCmd).trim})
  }

  lazy val State1 = State(sampleUC, None)
  lazy val State2a = State(MockUc2a.UC, None)
  lazy val State2b = State(MockUc2b.UC, None)
  lazy val State3 = State(MockUc3.UC, None)

  def UCE1 = new UseCaseEditor2().setState2(State1)
  def UCE2a = new UseCaseEditor2().setState2(State2a)
  def UCE2b = new UseCaseEditor2().setState2(State2b)
  def UCE3 = new UseCaseEditor2().setState2(State3)

  describe("Full-page rendering") {
    lazy val xml = inMockSession(UCE1.dispatch("render")(Templates.EntirePage))
    lazy val html = xml.toString

    it("should render the title") {
      html should include(sampleUC.header.title)
    }

    it("should render text fields") {
      html should include(TF1.defn.title)
      html should include(TF4.defn.title)
      html should include(">blah<") // TF1
      html should include(">hehe<") // TF3
    }


    it("should render NC field steps") {
      html should include("Normal")
      html should include("Alternative")
      html should include("I'm the title")
      html should include("Finally")
      html should include("7.0")
    }

    it("should render EC field steps") {
      html should include("Exceptions")
      html should include("EC-1E1")
      html should include("EC-1E1-1")
      html should include("EC-1E2")
      html should include("7.E.1")
    }
  }

  describe("setState()") {
    it("should use the given state") {
      val uce = UCE1
      uce.state should be theSameInstanceAs (State1)
    }
    // TODO rethink this. If state not in renderer, no need to recreate
    //    it("should replace the renderer") {
    //      val uce = new UseCaseEditor2()
    //      val r1 = uce.renderer
    //      uce.setState2(State1)
    //      uce.renderer should not be theSameInstanceAs(r1)
    //      uce.renderer.state should be(State1)
    //    }
  }

  describe("AJAX callbacks") {

    describe("title change") {
      lazy val (uce,resp) = new UseCaseEditor2().update2(_.updateTitle("  bananas  "))
      it("should update the editor state") {
        uce.uc.header.title should be("bananas")
      }
      it("should set the title via ajax") {
        assertIdAndActionR(resp, TitleId.asLocalId, """['"]bananas['"]""".r)
      }
    }

    describe("text field change") {
      lazy val (uce,resp) = new UseCaseEditor2().update2(TF1.updateText("bananas"))
      it("should update the editor state") {
        TF1(uce.fieldValues).text should be("bananas")
      }
      it("should set the field value via ajax") {
        resp should include("bananas")
        resp should include(uce.textFieldIds(TF1))
      }
    }

    describe("step field change") {
      lazy val (uce,resp) = UCE2b.update2(NCF.updateText(X1, "bananas --> 7.0.1"))
      it("should update the editor state") {
        NCF(uce.fieldValues).textmap(X1).text should startWith("bananas")
      }
      it("should set the field value via ajax") {
        resp should include("bananas ➡ [7.0.1]")
        resp should include(X1)
      }
      it("should update affected steps via ajax") {
        resp should include("⬅ [7.0]")
        resp should include(X3)
      }
      it("should not update unaffected steps via ajax") {
        resp should not include(X2)
      }
      it("should be able to affect the text of empty steps") {
        val (uce,resp) = UCE1.update2(NCF.updateText(NcSfv.tree(0).id, "bananas --> 7.0.2"))
        resp should include("⬅ [7.0]")
        resp should include(NcSfv.tree(0)(1).id)
      }
    }

    def assertIdAndAction(resp: String, id: LocalIdStr, actionStr: String): Unit =
      assertIdAndActionR(resp, id, Pattern.quote(actionStr).r)

    def assertIdAndActionR(resp: String, id: LocalIdStr, actionRegex: Regex): Unit =
      resp should include regex(s"""$id[^;\n]+?$actionRegex""")

    def assertIdRelabeled(resp: String, id: LocalIdStr, newLabel: String) {
      assertIdAndActionR(resp, s"$id-l".asLocalId, s"""['"]$newLabel['"]""".r)
    }

    def assertNewStepFound(resp: String) {
      resp should include("class=\"step\"")
      resp should include("<textarea")
    }

    def itRespectsMaxSteps(name: String, uceFn: => () => UseCaseEditor2, addFn: => UseCase => UcUpdateResult, labelAtMax: String) = {
      it(s"should not exceed the max-steps limit ($name)") {
        val uce = uceFn()
        var i=0
        def atMax = uce.uc.stepsAndLabels.get.ba.contains(labelAtMax.asLabel)
        while (!atMax && i<110) {
          i+=1
          val oldUc = uce.uc
          val (_, resp) = uce.update2(addFn)
          if (uce.uc eq oldUc) fail(s"AddFn didn't add. (attempt: $i)\nResponse: $resp\n${uce.uc.toPrettyString}")
        }
        val oldUc = uce.uc
        val (n,resp) = uce.update2(addFn)
        resp should include("alert(")
        uce.uc should be theSameInstanceAs(oldUc)
      }
    }

    describe("adding tail step") {
      def assertTailStepAdded(resp: String, lbl: String, coursesCss: String) {
        resp should include(s">$lbl<")
        resp should include(s"$coursesCss .addTailStep")
        assertNewStepFound(resp)
      }
      it("should add 7.2 for NC") {
        val (uce,resp) = UCE1.update2(NCF.addTailStep)
        stepTreeLens.get(uce.uc, NCF).nodes.size should be(3)
        assertTailStepAdded(resp, "7.2", ".courses-a")
      }
      it("should add 7.E.3 for NC") {
        val (uce,resp) = UCE1.update2(ECF.addTailStep)
        stepTreeLens.get(uce.uc, ECF).nodes.size should be(3)
        assertTailStepAdded(resp, "7.E.3", ".courses-e")
      }
      it("should not allow more steps than the max") {
        val (uce,resp) = UCE1.update2(ECF.addTailStep)
        stepTreeLens.get(uce.uc, ECF).nodes.size should be(3)
        assertTailStepAdded(resp, "7.E.3", ".courses-e")
      }
      it should behave like(itRespectsMaxSteps("NC", UCE1 _, NCF.addTailStep, "7.99"))
      it should behave like(itRespectsMaxSteps("EC", UCE1 _, ECF.addTailStep, "7.E.99"))
    }

    describe("adding a step") {
      lazy val (uce,resp) = UCE3.update2(NCF.addStep(X3))
      it("should update the state") {
        val ch = stepTreeLens.get(uce.uc, NCF)(0).children
        ch.size should be(4)
        ch.last.id should be(X5)
      }
      it("should push the new step to the client") {
        resp should include(X3)
        assertNewStepFound(resp)
      }
      it("should relabel proceeding steps") {
        assertIdRelabeled(resp, X2, "3") // 7.0.3
        assertIdRelabeled(resp, X5, "4") // 7.0.4
      }
      it("should update referencing text field text") {
        assertIdAndAction(resp, uce.textFieldIds(TF1), "[7.0.3]")
      }
      it("should update referencing step field text") {
        assertIdAndAction(resp, X1, "root [7.0.4]")
      }
      it should behave like(itRespectsMaxSteps("NC", UCE2b _, NCF.addStep(X3), "7.0.99"))
      it should behave like(itRespectsMaxSteps("EC", UCE1 _, ECF.addStep(EcSfv.tree(1).id), "7.E.2.99"))
    }

    describe("remove a step") {
      lazy val (uce,resp) = UCE3.update2(NCF.removeStep(X2))
      it("should update the state") {
        stepTreeLens.get(uce.uc, NCF).sizeRecursive should be(3)
      }
      it("should remove the step and its children via ajax") {
        assertIdAndAction(resp, X2, "remove")
        assertIdAndAction(resp, X4, "remove")
      }
      it("should relabel proceeding steps") {
        assertIdRelabeled(resp, X5, "2") // X5 -> 7.0.2
      }
      it("should update referencing text field text") {
        assertIdAndAction(resp, uce.textFieldIds(TF1), "DELETED")
      }
      it("should update referencing step field text") {
        assertIdAndAction(resp, X1, "root [7.0.2]")
      }
    }

    describe("indenting a step") {
      lazy val (uce,resp) = UCE3.update2(NCF.increaseIndent(X5))
      it("should update the state"){
        uce.uc.stepsAndLabels.get.ab(X5) should be("7.0.2.b")
      }
      it("should update the client"){
        assertIdAndAction(resp, X5, "attr")
        assertIdRelabeled(resp, X5, "b")
      }
      it("should not transition from AC to NC when node is 7.0.3") {
        resp should not include("ac_to_nc")
      }
      it("should transition from AC to NC when node is 7.1") {
        val (_,resp) = UCE1.update2(NCF.increaseIndent(NcSfv.tree(1).id))
        resp should include("ac_to_nc")
      }
      // TODO it("should fail when indent exceeds max steps"){
      // TODO it("should fail when indent exceeds max depth"){
    }

    describe("decreasing a step indent") {
      lazy val (uce,resp) = UCE3.update2(NCF.decreaseIndent(X5))
      it("should update the state"){
        uce.uc.stepsAndLabels.get.ab(X5) should be("7.1")
      }
      it("should update the client"){
        assertIdAndAction(resp, X5, "attr")
        assertIdRelabeled(resp, X5, "7.1")
      }
      it("should transition from NC to AC when creating 7.1") {
        resp should include("nc_to_ac")
      }
      it("should not transition from NC to AC when creating 7.0.3") {
        val (_,resp) = UCE3.update2(NCF.decreaseIndent(X4))
        resp should not include("nc_to_ac")
        assertIdAndAction(resp, X4, "attr")
      }
      it("should not transition from NC to AC when creating 7.E.2") {
        val id = EcSfv.tree(0)(0).id
        val (_,resp) = UCE1.update2(ECF.decreaseIndent(id))
        resp should not include("nc_to_ac")
        assertIdAndAction(resp, id, "attr")
      }
      // TODO it("should fail when indent exceeds max steps"){
    }
  }
}
