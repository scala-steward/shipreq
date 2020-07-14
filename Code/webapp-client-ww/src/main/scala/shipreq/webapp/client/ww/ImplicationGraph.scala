package shipreq.webapp.client.ww

import scala.annotation.tailrec
import shipreq.base.util._
import shipreq.webapp.base.data._
import shipreq.webapp.base.data.savedview.ImpGraphConfig.GraphDir
import shipreq.webapp.client.ww.GraphViz.DOT

object ImplicationGraph {
  import GraphUtil._

  def apply(focus: ReqId, fd: FilterDead, p: Project): DOT =
    apply(focus, fd, p.content.implications, p.content.reqs, p.config.reqTypes)

  def apply(focus   : ReqId,
            fd      : FilterDead,
            imps    : Implications.BiDir,
            reqs    : Requirements,
            reqTypes: ReqTypes): DOT =
    GraphViz.digraph { implicit b =>

      val impHelpers = new ImpHelpers(reqs, reqTypes)
      import impHelpers._

      def declareNode(id: ReqId): Unit = {
        node(id)
        declareId(id)
        b.labelAttr(pubid(id))
        deadNodeStyleIfDead(live(id))
      }

      val filterIdSet: Set[ReqId] => Set[ReqId] =
        fd(_)(live)

      val Focus = "F"
      val focusLive = live(focus)

      def traverse(dir: Direction) = {
        val graph    = imps(dir)
        val direct   = filterIdSet(graph(focus))
        val indirect = DeclAndFlow(List.newBuilder[ReqId], List.newBuilder[Content])

        def flow(from: String, fromLive: Live, to: ReqId, unconstrain: Boolean): Content =
          () => {
            b.flowS(from, dir, nodeName(to))

            if (fromLive.is(Dead) || live(to).is(Dead))
              deadLink()

            if (unconstrain)
              b append "[constraint=0]"
            else
              b.eol()
          }

        @tailrec
        def go(queue: List[ReqId], queueNext: Set[ReqId], seen: Set[ReqId]): Unit =
          queue match {
            case Nil =>
              if (queueNext.nonEmpty)
                go(queueNext.toList, Set.empty, seen)

            case fromId :: queue2 =>
              if (seen.contains(fromId))
                go(queue2, queueNext, seen)
              else {

                if (!direct.contains(fromId))
                  indirect.decl += fromId

                val toIds = filterIdSet(graph(fromId))
                for (toId <- toIds)
                  indirect.flow += flow(nodeName(fromId), live(fromId), toId, direct contains toId)

                go(queue2, queueNext ++ toIds, seen + fromId)
              }
          }

        go(Nil, direct, Set.empty)

        val d = DeclAndFlow(direct, direct.iterator.map(flow(Focus, focusLive, _, false)))
        val i = indirect.bimap(_.result(), _.result())
        DirectAndIndirect(d, i)
      }

      val forwards  = traverse(Forwards)
      val backwards = traverse(Backwards)

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

      b.rankdir(GraphDir.LeftToRight)
      styleSubsequentNodesAsImplications(Shape.Ellipse)

      // Focus
      b append Focus
      b append """[style=bold fillcolor="#cccccc" """
      b.setLabel(pubid(focus))
      b append ']'

      b append """node[fillcolor="#FFEDE2"]""";
      backwards.indirect.decl.foreach(declareNode)

      b.attrGroup("""rank=same;node[fillcolor="#FFC19C"]""")(
        backwards.direct.decl.foreach(declareNode))

      b.attrGroup("""rank=same;node[fillcolor="#7692B7" fontcolor=white]""")(
        forwards.direct.decl.foreach(declareNode))

      b append """node[fillcolor="#D6E1EF"]""";
      forwards.indirect.decl.foreach(declareNode)

      b append """edge[color="#FFC19C"]"""
      backwards.indirect.flow.foreach(_ ())

      b append """edge[color="#C27040"]"""
      backwards.direct.flow.foreach(_ ())

      b append """edge[color="#31537F"]"""
      forwards.direct.flow.foreach(_ ())

      b append """edge[color="#7692B7"]"""
      forwards.indirect.flow.foreach(_ ())
    }

}
