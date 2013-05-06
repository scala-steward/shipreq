package com.beardedlogic.usecase.lib

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.matchers.Matcher
import org.scalatest.matchers.MatchResult
import scala.collection.mutable.StringBuilder
import com.beardedlogic.usecase.test.TestHelpers

class StepTreeTest extends WordSpec with ShouldMatchers with TestHelpers {
  import NodeUtils._
  import StepTree._
  import TestHelpers.TreeDSL._

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
    val InitialTree = $("1.0" ~> $("1")).toStepNodes
    val BT_102 = $("a" ~> $("i", "ii", "iii"), "b", "c" ~> $("i", "ii"))
    val BT_103 = $("a" ~> $("i"), "b")
    val BigTree = $(
      "1.0" ~> $("1", "2" ~> BT_102, "3" ~> BT_103, "4"),
      "1.1" ~> $("1", "2", "3"),
      "1.2" ~> $("1", "2")
    ).toStepNodes
    val N = Step("N")
  }

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

  "incrementPosition()" should {
    val test = (lvl: Int, before: String, after: String) => {
      val B = StepNode("blah", lvl, before, null, Nil)
      val A = StepNode("blah", lvl, after, null, Nil)
      incrementPosition(B) should be(A)
    }
    "increase numeric positions" in {
      test(1, "1", "2")
      test(1, "9", "10")
      test(1, "15", "16")
    }
    "increase alpha positions" in {
      test(2, "a", "b")
      test(2, "h", "i")
      test(2, "z", "aa")
    }
    "increase roman positions" in {
      test(3, "i", "ii")
      test(3, "iii", "iv")
      test(3, "viii", "ix")
    }
  }

  // ------------------------------------------------------------------------------------------------------------------

  "insertStep()" when {
    import Steps._

    def test(afterId: String, nodes: List[StepNode], changes: NodeChange*) {
      val expected = removeIds(fixLevels(changeChildren(nodes, changes: _*)))
      val r = insertStep(N, afterId, nodes)
      removeIds(r._1) should matchTree(expected)
    }

    "tree is in initial state (1.0 & 1.0.1)" should {
      "insert before 1.0.1" in {
        test("1.0", InitialTree, "1.0" -> $("1/N", "2/Step:1"))
      }

      "insert after 1.0.1" in {
        test("1.0.1", InitialTree, "1.0" -> $("1", "2/N"))
      }
    }

    "tree is large and deep" when {
      "inserting after 1.0 (lvl 0) should create 1.0.1" in {
        test("1.0", BigTree, "1.0" -> $(
          "1/N",
          "2/Step:1",
          "3/Step:2" ~> BT_102,
          "4/Step:3" ~> BT_103,
          "5/Step:4"))
      }

      "inserting after 1.1 (lvl 0) should create 1.1.1" in {
        test("1.1", BigTree, "1.1" -> $("1/N", "2/Step:1", "3/Step:2", "4/Step:3"))
      }

      "inserting after 1.0.2 (lvl 1) should create 1.0.3" in {
        test("1.0.2", BigTree, "1.0" -> $(
          "1",
          "2" ~> BT_102,
          "3/N",
          "4/Step:3" ~> BT_103,
          "5/Step:4"))
      }

      "inserting after 1.1.2 (lvl 1) should create 1.1.3" in {
        test("1.1.2", BigTree, "1.1" -> $("1", "2", "3/N", "4/Step:3"))
      }

      "inserting after 1.0.2.a (lvl 2) should create 1.0.2.b" in {
        test("1.0.2.a", BigTree,
          "1.0.2" -> $("a" ~> $("i", "ii", "iii"), "b/N", "c/Step:b", "d/Step:c" ~> $("i", "ii"))
        )
      }

      "inserting after 1.0.2.a.i (lvl 3) should create 1.0.2.a.ii" in {
        test("1.0.2.a.i", BigTree, "1.0.2.a" -> $("i", "ii/N", "iii/Step:ii", "iv/Step:iii"))
      }

      "inserting after 1.0.2.c.ii (lvl 3) should create 1.0.2.c.iii" in {
        test("1.0.2.c.ii", BigTree, "1.0.2.c" -> $("i", "ii", "iii/N")
        )
      }
    }
  }

  /*
    --    1.0. Step:1.0
    [-      1. Step:1
    [>      2. Step:2
    <-        a. Step:a
    <-          i. Step:i
    <>          ii. Step:ii
    <>          iii. Step:iii
    <>        b. Step:b
    <>        c. Step:c
    <-          i. Step:i
    <>          ii. Step:ii
    [>      3. Step:3
    <-        a. Step:a
    <-          i. Step:i
    <>        b. Step:b
    [>      4. Step:4
    -}    1.1. Step:1.1
    [-      1. Step:1
    [>      2. Step:2
    [>      3. Step:3
    -}    1.2. Step:1.2
    [-      1. Step:1
    [>      2. Step:2

    Rules for increasing indent
      + Not first child

    Rules for decreasing indent
      + Not top-level

    Allow 1st<->2nd level? Yes.
    Allow 1.0 without 1.0.1? Yes.

    Buttons always exist. Control visibility with JS.
      $(".inc").show()
      $(".lvl-0 .inc").hide()

Test indent of 1.1.3
Test indent of 1.1.2
Test indent of 1.0.4
Test indent of 1.0.3.b
Test indent of 1.0.3
Test indent of 1.1
Test indent of 1.0.2

Test dec of 1.0.3.b
Test dec of 1.0.3.a
Test dec of 1.0.3.a.i
Test dec of 1.0.2.a
Test dec of 1.0.2.a.i
Test dec of 1.0.2.a.ii
Test dec of 1.0.2.a.iii
Test dec of 1.1.2
   */
}
