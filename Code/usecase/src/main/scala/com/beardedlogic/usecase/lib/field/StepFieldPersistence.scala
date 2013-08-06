package com.beardedlogic.usecase.lib.field

import com.beardedlogic.usecase.lib.{StepTree, StepNode}
import com.beardedlogic.usecase.lib.Types._
import com.beardedlogic.usecase.lib.tree.TreeOps._
import com.beardedlogic.usecase.model._
import StepField.StartingLabelIndices
import com.beardedlogic.usecase.lib.text.StepText

private[field] trait StepFieldValueLoader extends Field {
  this: StepField =>
  override type State = NormalisedStepTree

  override def load(loadCtx: FieldLoadCtx, mutableSaveCtx: MutableFieldSaveCtx) = {
    def unpackSteps(parentId: Long, level: Int, relations: Map[Long, List[Long]],
      stepData: Map[Long_StepValueId, (PlainValue[DataType.Step], String)]): List[NormalisedStep] = {

      relations.get(parentId).map(ids => {
        var labelIndex = sli.startingLabelIndex(level)
        ids.map(stepValueId => {
          val (stepValue, text) = stepData(stepValueId.tag[StepValueId])
          val children = unpackSteps(stepValueId, level + 1, relations, stepData)
          val localStepId = s"s$stepValueId".asLocalId
          val ns = NormalisedStep(localStepId, text.hasNormalisedRefs, children)
          mutableSaveCtx.stepValues += (localStepId -> stepValue)
          labelIndex += 1
          ns
        })
      }).getOrElse(List.empty[NormalisedStep])
    }

    val normalisedSteps = (
                            for {
                              fv <- loadCtx.fieldValues.get(rec.taggedId)
                              has <- loadCtx.relations.get(RelationType.Has)
                            } yield unpackSteps(fv.valueId, 0, has, loadCtx.stepData)
                            ).getOrElse(List.empty[NormalisedStep])
    NormalisedStepTree(normalisedSteps)
  }

  override def denormalise(normalisedState: NormalisedStepTree, savedSteps: SavedSteps) =
    if (normalisedState.isEmpty) {
      // Use field's default value
      val sfv = defaultValue
      (Some(sfv.tree), _ => sfv)

    } else {
      // Part 1: Turn normalisedStepTree into real StepTree
      val stepTree = StepTree(convertNodeTree[NormalisedStep, StepNode](
      normalisedState.nodes
      , {(node, level, index, children) => StepNode(node.id, level, index, children)}
      , sli.startingLabelIndex
      ))

      // Part 2: Later, create StepText for each node
      val part2 = (stepsAndLabels: StepAndLabelBiMap) => {
        val textmapBuilder = Map.newBuilder[LocalIdStr, StepText]
        stepTree.foreachRecursive(n => {
          val id = n.id
          val ntxt = normalisedState.stepMap(id).text
          textmapBuilder += (id -> StepText.load(id, ntxt)(savedSteps, stepsAndLabels))
        })
        StepFieldValue(this, stepTree, textmapBuilder.result)
      }

      (Some(stepTree), part2)
    }

  def defaultValue: Value
}

object StepFieldValueSaver {

  /**
   * Compares old and current states. When a difference is discovered a new `value` is inserted and stored in
   * `saveCtx.stepValues`. Where a step can be reused, it is.
   *
   * SIDE EFFECTS:
   * - `saveCtx.stepValues` << new & updated steps
   * - `dao` << new & updated steps
   *
   * @return Whether any changes were discovered.
   */
  def compareAndSaveChanges(
    dao: DAO,
    oldState: NormalisedStepTree,
    oldStepValues: Map[LocalIdStr, PlainValue[DataType.Step]],
    newState: NormalisedStepTree
    )(saveCtx: MutableFieldSaveCtx): Boolean = {

    val oldStateMap = oldState.stepMap
    def iter(newState: List[NormalisedStep]): Boolean = newState match {
      case curStep :: nextSiblings =>

        // Check children first
        var changeDetected = iter(curStep.children)

        // Check current node
        val oldStep = oldStateMap.get(curStep.id)
        val reusable = !changeDetected && oldStep.isDefined && oldStep.get == curStep
        // println(s"${curStep.text}(${curStep.id}) vs ${oldStep.map(_.text)}(${oldStep.map(_.id).getOrElse("")}) => reusable: ${reusable}")
        if (!reusable) {
          val newValue = oldStep.flatMap(ss => oldStepValues.get(ss.id)) match {
            case Some(oldStepValue) => dao.createValue(oldStepValue, LatestRev) // update
            case None => dao.createInitialValue(DataType.Step) // insert new
          }
          saveCtx.stepValues += (curStep.id -> newValue)
          changeDetected = true
        }

        // Check next sibling
        if (iter(nextSiblings)) changeDetected = true

        changeDetected

      case Nil => false
    }

    val changedDetected = iter(newState.nodes)
    changedDetected || {
      // Top-level order could be different and no changes would otherwise be detected
      oldState != newState
    }
  }
}

/**
 * Saves a StepField to the database.
 *
 * @param v The field value to save.
 */
class StepFieldValueSaver(val v: StepFieldValue, val fieldKeyRec: FieldKeyRec, val sli: StartingLabelIndices)
  extends FieldValueSaver[NormalisedStepTree] {

  import StepFieldValueSaver._

  type S = NormalisedStepTree

  def normalisedState(savedSteps: SavedSteps): S = {
    val normalisedSteps = convertNodeTree[StepNode, NormalisedStep](
      v.tree
      , (s, _, _, children) => NormalisedStep(s.id, v.getNormalisedText(s.id, savedSteps), children)
      , sli.startingLabelIndex
    )
    NormalisedStepTree(normalisedSteps)
  }

  override def record_required_? = v.tree.nonEmpty

  override def presave(dao: DAO, prevSave: Option[(FieldSaveCtx, S)], savedSteps: SavedSteps)(saveCtx: MutableFieldSaveCtx) = {
    lazy val nstate = normalisedState(savedSteps)

    prevSave match {
      // No previous save, add everything for first time
      case None =>
        v.tree.foreachRecursive(n => {
          val v = dao.createInitialValue(DataType.Step)
          saveCtx.stepValues += (n.id -> v)
        })
        true

      // Compare to previous and save deltas
      case Some((oldSaveCtx, oldFieldState)) =>
        compareAndSaveChanges(dao, oldFieldState, oldSaveCtx.stepValues, nstate)(saveCtx)
    }
  }

  override def save(dao: DAO, savedSteps: SavedSteps, combinedSaveCtx: FieldSaveCtx, newSaveCtx: FieldSaveCtx) = {
    val nstate = normalisedState(savedSteps)

    // Create steps
    for {
      (localId, v) <- newSaveCtx.stepValues
      ss <- nstate.stepMap.get(localId)
    } {
      dao.createStep(v, ss.text)
      for {
        (childState, i) <- ss.children.zipWithIndex
        childValue <- combinedSaveCtx.stepValues.get(childState.id)
      } dao.relate_stepParent_has_step(v, i.toShort, childValue)
    }

    // Link FV to top-level
    val fv = newSaveCtx.fieldValues(fieldKeyRec)
    for {
      (ss, i) <- nstate.zipWithIndex
      stepValue <- combinedSaveCtx.stepValues.get(ss.id)
    } dao.relate_stepParent_has_step(fv, i.toShort, stepValue)

    (None, nstate)
  }
}