package com.beardedlogic.usecase
package test

import org.scalatest.matchers.Matcher
import org.scalatest.matchers.MatchResult
import lib.NodeUtils._
import lib.StepTree._

/**
 * @since 30/04/2013
 */
trait TestHelpers {

  def eventually(cond: => Any) {
    val test = (sleep: Int) => try { cond; true } catch { case _: Throwable => Thread.sleep(sleep); false }
    if (!test(50))
      if (!test(50))
        if (!test(100))
          if (!test(100))
            if (!test(200))
              if (!test(500))
                cond
  }

  def matchTree(expected: List[StepNode]) = TestHelpers.TreeMatcher(expected)
}

object TestHelpers extends TestHelpers {

  case class TreeMatcher(expected: List[StepNode]) extends Matcher[List[StepNode]] {
    def apply(actual: List[StepNode]): MatchResult = {
      val result = actual == expected
      MatchResult(result,
        "Trees didn't match.\n" + inspectTrees("EXPECTED", expected, "ACTUAL", actual),
        "Trees matched but shouldn't have.\n" + inspectTree(actual))
    }
  }

  /**
   * Old way of generating trees.
   */
  object TreeDSL {
    
    case class NC(val node: String, val children: List[NC])
    def $(nodes: NC*) = nodes.toList
    implicit def nodeWithoutChildren(n: String) = NC(n, Nil)
    implicit class StringAsNode(val s: String) { def ~>(children: List[NC]) = NC(s, children) }
    implicit class NCListExt(val ncs: List[NC]) {
      val regex = """^(\S+?)/(\S+)$""".r
      def toStepNodes: List[StepNode] = toStepNodes(0, "", true)
      def toStepNodesN: List[StepNode] = toStepNodes(0, "", false)
      def toStepNodes(lvl: Int, idPrefix: String, genIds: Boolean): List[StepNode] = ncs.map { nc =>
        val (lbl, txt) = if (regex.pattern.matcher(nc.node).matches) {
          val regex(l, t) = nc.node; (l, t)
        } else
          (nc.node, "Step:" + nc.node)
        val id = idPrefix + lbl
        val ch = nc.children.toStepNodes(lvl + 1, id + ".", genIds)
        StepNode(if (genIds) id else null, lvl, lbl, Step(txt), ch)
      }
    }

    type NodeChange = Tuple2[String, List[NC]]
    def changeChildren(nodes: List[StepNode], changes: NodeChange*): List[StepNode] = nodes.map { n =>
      val matches = for ((id, c) <- changes if id == n.id) yield c
      val ch = if (matches.isEmpty) n.children else matches(0).toStepNodes
      n.copy(children = changeChildren(ch, changes: _*))
    }
    
  }
}