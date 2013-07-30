package com.beardedlogic.usecase.lib.text

import scala.collection.immutable.TreeSet
import com.beardedlogic.usecase.lib.change.Change
import com.beardedlogic.usecase.lib.change.Changes.{FlowToChange, FlowFromChange}
import com.beardedlogic.usecase.lib.Types._
import ParsingConfig.{FlowToStyle, FlowFromStyle, FlowStyle}

sealed trait Flow[Clause <: FlowClause] {

  def style: FlowStyle

  def createPotentiallyEmpty(refs: Map[LocalIdStr, LabelStr]): Clause

  def create(refs: Map[LocalIdStr, LabelStr]): Option[Clause] =
    if (refs.isEmpty) None
    else Some(createPotentiallyEmpty(refs))

  /** Returns a function that produces a Change to indicate that a flow clause was cleared. */
  val flowClearedChangeFn = createPotentiallyEmpty(Map.empty).flowChangeFn

  def toText(c: Clause) = style.makeFlowTextOrEmpty(c.sortedLabels)
}

sealed trait FlowClause {

  val refs: Map[LocalIdStr, LabelStr]

  /** Returns a function that produces a Change to indicate that a flow clause has changed. */
  def flowChangeFn: LocalIdStr => Change

  def sortedLabels: TreeSet[LabelStr] = {
    var s = TreeSet.empty[LabelStr]
    for (lbl <- refs.values) s += lbl
    s
  }
}

// ---------------------------------------------------------------------------------------------------------------------

object FlowFrom extends Flow[FlowFromClause] {
  override def style = FlowFromStyle
  override def createPotentiallyEmpty(refs: Map[LocalIdStr, LabelStr]) = FlowFromClause(refs)
}

case class FlowFromClause(refs: Map[LocalIdStr, LabelStr]) extends FlowClause {
  override def flowChangeFn = stepId => FlowFromChange(refs.keySet, stepId)
}

// ---------------------------------------------------------------------------------------------------------------------

object FlowTo extends Flow[FlowToClause] {
  override def style = FlowToStyle
  override def createPotentiallyEmpty(refs: Map[LocalIdStr, LabelStr]) = FlowToClause(refs)
}

case class FlowToClause(refs: Map[LocalIdStr, LabelStr]) extends FlowClause {
  override def flowChangeFn = stepId => FlowToChange(stepId, refs.keySet)
}
