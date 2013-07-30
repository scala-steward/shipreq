package com.beardedlogic.usecase.lib.change

import com.beardedlogic.usecase.lib.Types._
import com.beardedlogic.usecase.lib.{StepTree, StepNode}

object Changes {

  case class TitleChanged(before: String, after: String) extends Change

  case object TextChanged extends Change

  case class StepTextChanged(id: LocalIdStr) extends Change

  /** Indicates that one or more steps have changed. */
  // TODO Make this a ExistingStepLabelChanged trait
  case object StepTreeChanged extends Change

  case class TailStepAdded(node: StepNode) extends Change

  case class StepAdded(precedingNodeId: LocalIdStr, node: StepNode) extends Change

  case class StepRemoved(node: StepNode) extends Change

  case class StepIndentIncreased(node: StepNode, oldTree: StepTree) extends Change

  case class StepIndentDecreased(node: StepNode, oldTree: StepTree) extends Change

  /**
   * Indicates that a step's flow-from list has changed.
   *
   * Example: If the text of step 1.7 changes from `"Blah"` or `"Blah ⬅ 1.0.2"` to `"Blah ⬅ 1.3, 1.4"`
   * then this message will be broadcast:
   * {{{
   * FlowToChange( [1.3, 1.4], 1.7 )
   * }}}
   *
   * @param fromIds The IDs of all steps that now flow to the target.
   * @param toId The ID of the step that issued the change, the step to which the from-steps now flow.
   */
  case class FlowFromChange(fromIds: Set[LocalIdStr], toId: LocalIdStr) extends Change

  /**
   * Indicates that a step's flow-to list has changed.
   *
   * Example: If the text of step 1.7 changes from `"Blah"` or `"Blah ➡ 1.0.2"` to `"Blah ➡ 1.3, 1.4"`
   * then this message will be broadcast:
   * {{{
   * FlowToChange( 1.7, [1.3, 1.4] )
   * }}}
   *
   * @param fromId The ID of the step that issued the change, the step from which steps now flow out.
   * @param toIds The IDs of all steps that the source step now flows to.
   */
  case class FlowToChange(fromId: LocalIdStr, toIds: Set[LocalIdStr]) extends Change

}
