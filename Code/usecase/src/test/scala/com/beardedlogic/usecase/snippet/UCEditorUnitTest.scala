package com.beardedlogic.usecase.snippet

import UCEditor.{Step, StepNode, flattenNodes, incrementPosition, insertStep}
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import scala.annotation.tailrec

class UCEditorUnitTest extends WordSpec with ShouldMatchers {
  import UCEditor._

  /**
   * StepNode test data.
   */
  object StepNodes {
    val A = StepNode("id.A", 1, "1", null, Nil)
    val B = StepNode("id.B", 1, "2", null, Nil)
    val C = StepNode("id.C", 1, "3", null, Nil)
    val D = StepNode("id.D", 1, "4", null, Nil)
    val ABCD = A :: B :: C :: D :: Nil

    val A2 = StepNode("id.A", 1, "2", null, Nil)
    val B2 = StepNode("id.B", 1, "3", null, Nil)
    val C2 = StepNode("id.C", 1, "4", null, Nil)
    val D2 = StepNode("id.D", 1, "5", null, Nil)
    val ABCD2 = A2 :: B2 :: C2 :: D2 :: Nil

    val N = StepNode("id.N", 1, "N", null, Nil)
  }

  /**
   * Step test data.
   */
  object Steps {
    val A = Step("A*")
    val A1 = Step("A.1")
    val A2 = Step("A.2")
    val A3 = Step("A.3")
    val A4 = Step("A.4")
    val B = Step("B*")
    val C = Step("C*")
    val N = Step("N")

    val `1.0 & 1.0.1` = StepNode("id.1.0", 0, "1.0", A, StepNode("id.1.0.1", 1, "1", B, Nil) :: Nil) :: Nil
    val `1.0.[1234]` = nodeRow(1, "1.0.", (1, A1), (2, A2), (3, A3), (4, A4))
    val `1.0 & 1.0.[1234]` = StepNode("id.1.0", 0, "1.0", A, `1.0.[1234]`) :: Nil
  }

  /**
   * Builds a row of nodes.
   */
  def nodeRow(lvl: Int,
              idPrefix: String,
              nodes: Tuple2[Any, Step]*): List[StepNode] =
    List((
      for ((lbl, step) <- nodes)
        yield StepNode(if (idPrefix == null) null else idPrefix + lbl.toString, lvl, lbl.toString, step, Nil)
    ): _*)

  /**
   * Recursively sets all IDs to null.
   */
  def removeIds(l: List[StepNode]): List[StepNode] = l.map((n) => n.copy(id = null, children = removeIds(n.children)))

  // -------------------------------------------------------------------------------------------------------------------

  "flattenNodes()" should {
    "flatten recursively" in {

      val c1_0_2_x =
        StepNode("1.0.2.a", 2, "a", null, Nil) ::
          StepNode("1.0.2.b", 2, "b", null, Nil) ::
          Nil

      val c1_0_x =
        StepNode("1.0.1", 1, "1", null, Nil) ::
          StepNode("1.0.2", 1, "2", null, c1_0_2_x) ::
          StepNode("1.0.3", 1, "3", null, Nil) ::
          Nil

      val c1_2_x =
        StepNode("1.2.1", 1, "1", null, Nil) ::
          Nil

      val top =
        StepNode("1.0", 0, "1.0", null, c1_0_x) ::
          StepNode("1.1", 0, "1.1", null, Nil) ::
          StepNode("1.2", 0, "1.1", null, c1_2_x) ::
          Nil

      flattenNodes(top).map(_.id) should be(List(
        "1.0", "1.0.1", "1.0.2", "1.0.2.a", "1.0.2.b", "1.0.3",
        "1.1",
        "1.2", "1.2.1"))
    }
  }

  "incrementPosition(List)" should {
    import StepNodes._
    "do nothing with an empty list" in { incrementPosition(Nil) should be(Nil) }
    "increment the position of all items" in { incrementPosition(ABCD) should be(ABCD2) }
  }

  /*
  "insertNode()" should {
    import StepNodes._
    "insert at the beginning" in { insertNode(None, N, ABCD) should be(N :: ABCD2) }
    "insert after first" in { insertNode(Some(A), N, ABCD) should be(A :: N :: B2 :: C2 :: D2 :: Nil) }
    "insert in the middle" in { insertNode(Some(B), N, ABCD) should be(A :: B :: N :: C2 :: D2 :: Nil) }
    "insert at the end" in { insertNode(Some(D), N, ABCD) should be(ABCD :+ N) }
  }
  */

  "insertStep()" when {
    import Steps._

    "tree is 1.0 & 1.0.1" should {
      "insert before 1.0.1" in {
        removeIds(insertStep(N, "id.1.0", `1.0 & 1.0.1`)._1) should be(
          StepNode(null, 0, "1.0", A, nodeRow(1, null, (1, N), (2, B))) :: Nil
        )
      }
      "insert after 1.0.1" in {
        removeIds(insertStep(N, "id.1.0.1", `1.0 & 1.0.1`)._1) should be(
          StepNode(null, 0, "1.0", A, nodeRow(1, null, (1, B), (2, N))) :: Nil
        )
      }
    }
  }

}