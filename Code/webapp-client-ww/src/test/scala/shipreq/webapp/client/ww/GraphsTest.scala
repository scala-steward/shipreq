package shipreq.webapp.client.ww

import utest._
import shipreq.base.test.BaseTestUtil._
import shipreq.webapp.base.test.SampleProject6
import GraphViz.DOT

object GraphsTest extends TestSuite {

  val _normalise = "([\\]{;])".r

  def normaliseDOT(d: DOT): String =
    _normalise.replaceAllIn(d.content, "$1\n")

  def assertDOT(actual: DOT, expect: DOT): Unit = {
    val a = normaliseDOT(actual)
    val e = normaliseDOT(DOT(expect.content.trim.replaceAll("\n *", "")))
    assertMultiline(a, e)
  }

  // TODO Test Graphs.useCaseStepFlow with more complicated flow

  override def tests = TestSuite {
    'stepFlow {
      import SampleProject6._, Values._
      val actual = Graphs.useCaseStepFlow(uc1, project.reqs.useCases)
      val expect = DOT(
        """
          |digraph G{rankdir=LR;ranksep=0.28;
          |
          |S[shape=circle style=filled color=black fontsize=1 height=.3]
          |E[shape=doublecircle style=filled color=black fontsize=1 height=.3]
          |
          |{node[fillcolor=lawngreen style=filled shape=invhouse]
          |  10[label="1.0"]
          |}
          |{node[fillcolor=lawngreen style=filled shape=ellipse]
          |  11[label="1.0.1"]
          |  12[label="1.0.2"]
          |  19[label="1.0.2.a"]
          |  13[label="1.0.3"]
          |}
          |
          |{node[fillcolor=skyblue style=filled shape=invhouse]
          |  14[label="1.1"]
          |}
          |{node[fillcolor=skyblue style="filled,rounded" shape=box]
          |  15[label="1.1.1"]
          |}
          |
          |{node[fillcolor=tomato style=filled shape=octagon]
          |  18[label="1.E.1"]
          |}
          |
          |{edge[weight=9]S->10->11->12->19->13->E}
          |14->15;
          |18;
          |
          |15->12;
          |13->11;
          |
          |}
        """.stripMargin)
      assertDOT(actual, expect)
    }
  }
}
