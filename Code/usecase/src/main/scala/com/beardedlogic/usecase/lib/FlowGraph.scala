package com.beardedlogic.usecase.lib

import scalaz.{Cord, Monoid, Foldable, Functor, NonEmptyList}
import scalaz.std.list.listInstance
import scalaz.std.option.optionInstance
import scalaz.syntax.foldable._
import scalaz.syntax.functor._
import scalaz.syntax.monoid._
import field.{StepFieldValue, ExceptionCourseField, NormalCourseField}
import Types._

/**
 * Creates a DOT graph of the steps in a UseCase, and how one flows through them.
 * The DOT graph can then be turned into a graphic via Graphviz, Viz.js, et al.
 *
 * Usage: `render(model(uc))`
 */
object FlowGraph {

  // =====================================================================================================================
  // Data types

  type Node = LabelStr
  type ExplicitFlow = (Node, Node)
  type ImplicitFlow = NonEmptyList[Node]

  sealed trait Category {
    def render(d: IntraCatData): Cord
  }

  case class FlowGraphModel(
    intraCatData: Map[Category, IntraCatData],
    socialData: SocialData)

  case class IntraCatData(
    headNodes: List[Node],
    implicitFlows: List[ImplicitFlow])

  case class SocialData(
    explicitFlows: List[ExplicitFlow],
    startNodes: List[Node],
    endNodes: List[Node])

  implicit object SocialDataMonoid extends Monoid[SocialData] {
    override val zero = SocialData(List.empty, List.empty, List.empty)
    override def append(a: SocialData, b2: => SocialData) = {
      val b = b2
      SocialData(
        a.explicitFlows ++ b.explicitFlows,
        a.startNodes ++ b.startNodes,
        a.endNodes ++ b.endNodes
      )
    }
  }

  implicit object FlowGraphModelMonoid extends Monoid[FlowGraphModel] {
    override val zero = FlowGraphModel(Map.empty, SocialDataMonoid.zero)
    override def append(a: FlowGraphModel, b2: => FlowGraphModel) = {
      val b = b2
      if (a eq zero) b
      else if (b eq zero) a
      else FlowGraphModel(
        a.intraCatData ++ b.intraCatData,
        a.socialData |+| b.socialData)
    }
  }

  // ===================================================================================================================
  // Model generation

  object Modeller {
    import Renderer.{NC, AC, EC}
    import FlowGraphModelMonoid.zero
    import StepTreeZipper._

    implicit def focus2node(f: AnyFocus): Node = f.label
    implicit def focusS2nodeS[F <: AnyFocus](s: List[F]): List[Node] = s map focus2node
    //implicit def focusF2nodeS[F[_]: Functor](f: F[AnyFocus]): List[Node] = f map focus2node toList

    def model(uc: UseCase): FlowGraphModel = {
      val labels = uc.stepsAndLabels.get.ab
      def zipBuilder(sfv: StepFieldValue) = DeepBuilder(sfv.textmap, labels)

      uc.fieldValues.toList foldMap {
        case (f: NormalCourseField, fv) =>
          val sfv = f.castValue(fv)
          val b = zipBuilder(sfv)
          val ncNode :: acNodes = sfv.tree.nodes
          val nc = processZ(NC, b.build(ncNode, Nil))
          val ac = processUnlessEmpty(AC, acNodes, b)
          nc |+| ac

        case (f: ExceptionCourseField, fv) =>
          val sfv = f.castValue(fv)
          processUnlessEmpty(EC, sfv.tree.nodes, zipBuilder(sfv))

        case _ => zero
      }
    }

    private def processUnlessEmpty(c: Category, nodes: List[StepNode], b: => DeepBuilder) = nodes match {
      case Nil => zero
      case h :: t => processZ(c, b.build(h, t))
    }

    private def processZ(implicit c: Category, dz: DeepZipper): FlowGraphModel = {
      implicit val fzs = flattenTopNodes(dz)
      val i = IntraCatData(headNodes, implicitFlows)
      val sd = SocialData(explicitFlows, startNodes, endNodes)
      FlowGraphModel(Map(c -> i), sd)
    }

    def flattenTopNodes(dz: DeepZipper): List[FlatZipper] = dz map (_.flat) toList

    def headNodes(implicit z: DeepZipper): List[Node] = z.toList

    def implicitFlows(implicit tops: List[FlatZipper]): List[ImplicitFlow] = tops map implicitFlow
    def implicitFlow(z: FlatZipper): ImplicitFlow = NonEmptyList[Node](z.focus, z.rights map focus2node: _*)

    def explicitFlows(implicit tops: List[FlatZipper]): List[ExplicitFlow] = tops map explicitFlow flatten
    def explicitFlow(z: FlatZipper): List[ExplicitFlow] = z.toList map explicitFlow flatten
    def explicitFlow(y: AnyFocus): List[ExplicitFlow] =
      y.flowToClause map (_.refs.values.toList strengthL focus2node(y)) getOrElse List.empty

    def startNodes(implicit c: Category, dz: DeepZipper): List[Node] = c match {
      case NC => List(dz.focus.label)
      case AC | EC => dz.toList filter (_.flowFromClause.isEmpty)
    }

    def endNodes(implicit tops: List[FlatZipper]): List[Node] = tops map (_.end.focus) filter (_.flowToClause.isEmpty)
  }

  // ===================================================================================================================
  // Rendering

  object Renderer {
    val ToSymbol = Cord("->")
    val SepSymbol = Cord(";")
    val GrpStart = Cord("{")
    val GrpEnd = Cord("}")

    val StartSymbol = Cord("S")
    val StartDecl = StartSymbol ++ Cord("[shape=circle style=filled color=black fontsize=1 height=.3]")
    val EndSymbol = Cord("E")
    val EndDecl = EndSymbol ++ Cord("[shape=doublecircle style=filled color=black fontsize=1 height=.3]")

    val GraphGroup = group(Cord("digraph G{ranksep=0.28;")) _
    //val NcGroup = group(Cord("subgraph clusterN{style=invis edge[weight=9] node[style=filled fillcolor=lawngreen shape=ellipse]")) _
    val NcGroup = group(Cord("{edge[weight=9] node[style=filled fillcolor=lawngreen shape=ellipse]")) _
    val AcGroup = anonGroup(Cord("""node[style="filled,rounded" fillcolor=skyblue shape=box]""")) _
    val EcGroup = anonGroup(Cord("node[style=filled fillcolor=tomato shape=octagon]")) _
    val NcHeadNodeGroup = anonGroup(Cord("node[shape=invhouse]")) _
    val AcHeadNodeGroup = anonGroup(Cord("node[style=filled shape=invhouse]")) _
    val TerminalsGroup = anonGroup("edge[weight=9]") _

    case object NC extends Category {
      override def render(d: IntraCatData) =
        NcGroup(
          NcHeadNodeGroup(nodeDecls(d.headNodes)) ++
          renderI(d.implicitFlows))
    }

    case object AC extends Category {
      override def render(d: IntraCatData) =
        AcGroup(
          AcHeadNodeGroup(nodeDecls(d.headNodes)) ++
          renderI(d.implicitFlows))
    }

    case object EC extends Category {
      override def render(d: IntraCatData) =
        EcGroup(renderI(d.implicitFlows))
    }

    def mapIfNonEmpty(c: Cord)(f: Cord => Cord): Cord = if (c.size == 0) c else f(c)

    def group(start: => Cord)(inner: Cord): Cord = mapIfNonEmpty(inner)(start ++ _ ++ GrpEnd)

    def anonGroup(cust: => Cord)(inner: Cord): Cord = group(GrpStart ++ cust)(inner)

    def nodeDotId(n: Node): Cord = '"' -: Cord(n) :- '"'

    def mkStmts[M[_] : Functor : Foldable, A](ma: M[A])(f: A => Cord): Cord = ma map f intercalate SepSymbol

    // Eg. a;b;c
    def nodeDecls(nodes: List[Node]): Cord = nodes match {
      case Nil => Cord.empty
      case h :: Nil => nodeDotId(h)
      case _ => GrpStart ++ mkStmts(nodes)(nodeDotId) ++ GrpEnd
    }

    def renderE(eflows: List[ExplicitFlow]): Cord = mkStmts(eflows)(renderE)

    def renderE(e: ExplicitFlow): Cord = nodeDotId(e._1) ++ ToSymbol ++ nodeDotId(e._2)

    def renderI(iflows: List[ImplicitFlow]): Cord = mkStmts(iflows)(renderI)

    def renderI(iflow: ImplicitFlow): Cord = iflow map nodeDotId intercalate ToSymbol

    def renderC(m: Map[Category, IntraCatData])(c: Category): Cord =
      m.get(c) match {
        case None => Cord.empty
        case Some(d) => c.render(d)
      }

    def renderTerminals(d: SocialData): Cord =
      TerminalsGroup(
        mapIfNonEmpty(nodeDecls(d.startNodes))(StartSymbol ++ ToSymbol ++ _ ++ SepSymbol) ++
        mapIfNonEmpty(nodeDecls(d.endNodes))(_ ++ ToSymbol ++ EndSymbol)
      )

    def render(m: FlowGraphModel): Cord = {
      val renderCat = renderC(m.intraCatData) _
      GraphGroup(
        renderCat(NC) ++
        renderCat(AC) ++
        renderCat(EC) ++
        renderE(m.socialData.explicitFlows) ++ SepSymbol ++
        StartDecl ++ EndDecl ++
        renderTerminals(m.socialData)
      )
    }
  }
}