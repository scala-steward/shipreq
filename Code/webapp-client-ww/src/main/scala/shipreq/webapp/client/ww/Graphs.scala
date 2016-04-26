package shipreq.webapp.client.ww

import scala.collection.mutable
import shipreq.base.util.ScalaExt._
import shipreq.base.util.{Digraph, Valid}
import shipreq.base.util.VectorTree.PartialLocation
import shipreq.webapp.base.data._
import GraphViz.DOT

object Graphs {

  private def digraph(f: StringBuilder => Unit): DOT = {
    implicit val sb = new StringBuilder
    group("digraph G")(f(sb))
    DOT(sb.result())
  }

  private def group(group: String)(inner: => Unit)(implicit sb: StringBuilder): Unit = {
    sb append group
    sb append '{'
    inner
    sb append '}'
  }

  private def withAttr(attr: String)(inner: => Unit)(implicit sb: StringBuilder): Unit = {
    sb append '{'
    sb append attr
    inner
    sb append '}'
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  private final val StartNode = "S"
  private final val EndNode   = "E"

  /**
   * Creates a graph of the flow of steps in a given UseCase.
   *
   * TODO Currently only graphs intra-usecase flow. Flow to or from other UseCases is currently ignored.
   */
  def useCaseStepFlow(id: UseCaseId, useCases: UseCases): DOT = {
    import StaticField.{NormalAltStepTree => NA, ExceptionStepTree => E, UseCaseStepTree => F}

    val uc      = useCases.imap.need(id)
    val stepsNA = NA.useCaseSteps get uc
    val stepsE  = E .useCaseSteps get uc
    val flow    = useCases.stepFlow.forwards: Digraph.UniDir[UseCaseStepId]

    digraph { implicit sb =>
      sb append "rankdir=LR;ranksep=0.28;"

      val terminalStyleEnd = " style=filled color=black fontsize=1 height=.3]"
      def startNode(): Unit = {
        sb append StartNode
        sb append "[shape=circle"
        sb append terminalStyleEnd
      }

      def endNode(): Unit = {
        sb append EndNode
        sb append "[shape=doublecircle"
        sb append terminalStyleEnd
      }

      val _nodes = mutable.Map.empty[UseCaseStepId, String]
      def getNode(id: UseCaseStepId): Option[String] = _nodes.get(id)
      def register(id: UseCaseStepId, node: String): Unit = _nodes.update(id, node)

      def initSubtreeNodes(steps: UseCaseSteps, field: F, tf: UseCaseSteps.Tree => Range): Iterator[(PartialLocation, () => Unit)] = {
        steps.tree.subtreeLocAndValueIterator[(PartialLocation, () => Unit)](tf(steps.tree), (loc, step) => {
          val ploc = steps.partialLocs.forward(loc)
          if (ploc.validity :: Valid) {
            val label = field.stepLabel(uc.pos, ploc, mnemonicPrefix = false)
            val node = step.id.value.toString
            register(step.id, node)
            val nodeDOT: () => Unit = () => {
              sb append node
              sb append "[label=\""
              sb append label
              sb append "\"]"
            }
            (ploc, nodeDOT)
          } else
            null
        }).filter(_ ne null)
      }

      def initSubtreeNodesHT(headAttr: String, tailAttr: String, ns: Iterator[(PartialLocation, () => Unit)]): Unit = {
        val h = Vector.newBuilder[() => Unit]
        val t = Vector.newBuilder[() => Unit]
        for (x <- ns)
          (if (x._1.value.tail.isEmpty) h else t) += x._2
        execWithAttr(headAttr, h.result())
        execWithAttr(tailAttr, t.result())
      }

      def execWithAttr(attr: String, fs: TraversableOnce[() => Unit]): Unit =
        if (fs.nonEmpty)
          withAttr(attr)(fs.foreach(_()))

      def implicitFlow(steps: UseCaseSteps, field: F, tf: UseCaseSteps.Tree => Range): Unit = {
        var first = true
        steps.tree.subtreeLocAndValueIterator(tf(steps.tree), (loc, step) =>
          for (node <- getNode(step.id)) {

            if (loc.tail.isEmpty) {
              // Beginning of a new flow
              if (first)
                first = false
              else
                sb append ';'
            } else
              // Flow continuation
              sb append "->"

            sb append node
          }
        ).drain()
      }

      def explicitFlow(tree: UseCaseSteps.Tree): Unit =
        for {
          fromStep <- tree.valueIterator
          fromNode <- getNode(fromStep.id)
          toStepId <- flow(fromStep.id)
          toNode   <- getNode(toStepId)
        } {
          sb append fromNode
          sb append "->"
          sb append toNode
          sb append ';'
        }

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

      startNode()
      endNode()

      initSubtreeNodesHT(
        "node[fillcolor=lawngreen style=filled shape=invhouse]",
        "node[fillcolor=lawngreen style=filled shape=ellipse]",
        initSubtreeNodes(stepsNA, NA, NA.treeFilterN))

      initSubtreeNodesHT(
        "node[fillcolor=skyblue style=filled shape=invhouse]",
        "node[fillcolor=skyblue style=\"filled,rounded\" shape=box]",
        initSubtreeNodes(stepsNA, NA, NA.treeFilterA))

      execWithAttr(
        "node[fillcolor=tomato style=filled shape=octagon]",
        initSubtreeNodes(stepsE, E, E.treeFilter).map(_._2))

      withAttr("edge[weight=9]") {
        sb append StartNode
        sb append "->"
        implicitFlow(stepsNA, NA, NA.treeFilterN)
        sb append "->"
        sb append EndNode
      }

      implicitFlow(stepsNA, NA, NA.treeFilterA); sb append ';'
      implicitFlow(stepsE , E , E .treeFilter ); sb append ';'

      explicitFlow(stepsNA.tree)
      explicitFlow(stepsE .tree)
    }
  }
}
