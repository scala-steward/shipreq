package com.beardedlogic.usecase
package lib

import scala.reflect.ClassTag
import scalaz.NonEmptyList
import Types._
import change._
import field._
import text.FreeText
import model._
import util.{AppliedLens, LazyVal, BiMap}
import Changes._
import text.ParsingUtils._
import tree.TreeOps._

object UseCaseFns {

  def addChangesToResult(beforeTransform: UseCase, transformResult: UcUpdateResult, changes: NonEmptyList[(UcChangeDomain, Change)]): UcUpdateResult =
    transformResult match {
      case NoChange => Changed(beforeTransform, changes)
      case Changed(v,c) => Changed(v, c append changes)
      case f@ChangeFailure(_) => f
    }

  def keyByField(f: Field)(c: Change) = (f -> c)

  def filter[F <: Field](fields: List[Field])(implicit m: ClassTag[F]): List[F] =
    fields.filter(_.getClass.isAssignableFrom(m.runtimeClass)).asInstanceOf[List[F]]

  // TODO Minimise computation with savedSteps + StepAndLabelBiMap

  def generateSavedSteps(saveCtx: FieldSaveCtx): SavedSteps =
    BiMap(saveCtx.stepValues.map {case (localStepId, stepValue) => (stepValue.taggedDataId -> localStepId)})

  def mergeStepAndLabelMaps(maps: Iterable[Map[LocalIdStr, LabelStr]]): Map[LocalIdStr, LabelStr] =
    (Map.empty[LocalIdStr, LabelStr] /: maps)(_ ++ _)

  def generateStepAndLabelBiMap(maps: Iterable[Map[LocalIdStr, LabelStr]]): StepAndLabelBiMap =
    LazyVal(() => BiMap(mergeStepAndLabelMaps(maps)))

  def generateStepAndLabelBiMap(fieldValues: FieldValues, uch: UseCaseHeader): StepAndLabelBiMap =
    generateStepAndLabelBiMap(extractStepAndLabelMaps(fieldValues, uch))

  def generateStepAndLabelBiMap(uc: UseCase): StepAndLabelBiMap = generateStepAndLabelBiMap(uc.fieldValues, uc.header)

  def extractStepAndLabelMaps(fieldValues: FieldValues, uch: UseCaseHeader): Iterable[Map[LocalIdStr, LabelStr]] =
    fieldValues.map {
      case (f, v: StepFieldValue) => generateStepAndLabelMap(f, v.tree, uch)
      case _ => Map.empty[LocalIdStr, LabelStr]
    }

  def generateStepAndLabelMap(field: Field, tree: StepTree, uch: UseCaseHeader): Map[LocalIdStr, LabelStr] =
    field match {
      case f: StepField => mapIdsToFullLabels(tree.nodes, f.rootLabelPrefix(uch))
      case f => throw new IllegalStateException(s"Don't know how to mapIdsToFullLabels for field: $f")
    }

  def load(ucRec: UseCaseRec, dao: DAO, lock: Locks.ReadLockToken): UseCaseSaveCheckpoint = {
    val fieldList = Defaults.FieldList.get.fields // TODO hardcoded fieldlist

    // Load use case fields
    val (saveCtx, fieldStates) = {
      val mutableSaveCtx = new MutableFieldSaveCtx
      val loadCtx = dao.getFieldLoadCtxFor(ucRec.valueId)
      val fieldStates = Map.newBuilder[Field, Field#State]
      for (f <- fieldList) {
        // Load field states
        // (saveCtx.stepValues populated here by field-loaders)
        val fs = f.load(loadCtx, mutableSaveCtx)
        fieldStates += (f -> fs)

        // Add field values to saveCtx
        for (fvrec <- loadCtx.fieldValues.get(f.rec.taggedId)) mutableSaveCtx.fieldValues += (f.rec -> fvrec)
      }
      (mutableSaveCtx.immutable, fieldStates.result)
    }

    // Assemble and denormalise
    implicit val savedSteps = generateSavedSteps(saveCtx)

    var stepAndLabelMaps = List.empty[Map[LocalIdStr, LabelStr]]
    val fieldValueFns = Map.newBuilder[Field, StepAndLabelBiMap => Field#Value]
    for ((f, s) <- fieldStates) {
      val (stepTreeOp, fn) = f.denormalise(f.castState(s), savedSteps)
      stepTreeOp.map(stepAndLabelMaps :+= generateStepAndLabelMap(f, _, ucRec.header))
      fieldValueFns += (f -> fn)
    }
    val stepsAndLabels = generateStepAndLabelBiMap(stepAndLabelMaps)

    val fieldValues = for ((f, fn) <- fieldValueFns.result) yield (f -> fn(stepsAndLabels))

    // Final results
    val uc = UseCase(ucRec.header, fieldList, fieldValues, stepsAndLabels)
    UseCaseSaveCheckpoint(uc, ucRec.value, saveCtx, fieldStates, savedSteps)
  }

  /**
   * Saves the use case.
   *
   * Does nothing if there are differences between the current UC, and the last-saved revision.
   *
   * @return A checkpoint is there was anything to save, else `None` if UC was already up-to-date.
   */
  def save(uc: UseCase, prevSave: Option[UseCaseSaveCheckpoint], dao: DAO): Option[UseCaseSaveCheckpoint] = {
    // TODO refactor save()

    def actuallySave: Option[UseCaseSaveCheckpoint] = dao.withTransaction {
      var changesDetected = false
      val saveCtx1 = new MutableFieldSaveCtx
      var UCsFVs = Set.empty[Value[DataType.FieldValue]]
      var removeOldFVs = Set.empty[PlainValue[DataType.FieldValue]]
      val oldSavedSteps = prevSave.map(_.savedSteps).getOrElse(BiMap.empty)

      // Check fields for changes and presave
      for ((field, fv_) <- uc.fieldValues) {
        val fv = field.castValue(fv_)
        val saver = field.valueSaver(fv) // TODO reuse
        val fkrec = field.rec
        val oldFV: Option[PlainValue[DataType.FieldValue]] = prevSave.flatMap(_.saveCtx.fieldValues.get(fkrec))
        trace(s"$field - fkrec=$fkrec, oldFV=$oldFV")

        // Check if field has anything to save
        if (!saver.record_required_?) {
          if (oldFV.isDefined) {
            changesDetected = true
            removeOldFVs += oldFV.get
            trace(s"$field - Nothing to save anymore. Used to be, thus removal required.")
          }
        } else {
          // Compare state and presave
          val previous = for {
            ls <- prevSave
            fs <- ls.fieldStates.get(field)
          } yield (ls.saveCtx, field.castState(fs))
          val fieldChanged = saver.presave(dao, previous, oldSavedSteps)(saveCtx1)
          if (fieldChanged) {
            // Field changed, presave a new field value
            val newValue = if (oldFV.isEmpty)
              dao.createInitialValue(DataType.FieldValue)
            else
              dao.createValue(oldFV.get, LatestRev)
            trace(s"$field - New value created: rev=${newValue.rev}, id=${newValue.valueId}")
            saveCtx1.fieldValues += (fkrec -> newValue)
            changesDetected = true
            UCsFVs += newValue
          } else {
            // Reuse the existing field value
            oldFV.foreach(UCsFVs += _)
            trace(s"$field - Reuse.")
          }
        }
      }

      // Check for changes to the use case itself
      changesDetected ||= prevSave.map(_.uc.header != uc.header).getOrElse(true)

      if (!changesDetected) None
      else {
        val saveCtx2 = saveCtx1.immutable
        val combinedSaveCtx = if (prevSave.isEmpty) saveCtx2 else saveCtx2.combineWith(prevSave.get.saveCtx)
        val savedSteps = generateSavedSteps(combinedSaveCtx)

        // Create new usecase
        val ucValue = if (prevSave.isEmpty)
          dao.createInitialValue(DataType.UseCase)
        else
          dao.createValue(prevSave.get.rec, LatestRev)
        // TODO using default FieldList
        val ucRec = dao.createUseCase(ucValue, uc.header, Defaults.FieldList.get)

        // Save new field values
        var newFieldStates = prevSave.map(_.fieldStates).getOrElse(Map.empty)
        for {
          (field, fv_) <- uc.fieldValues
          fvRec <- saveCtx2.fieldValues.get(field.rec)
        } {
          val fv = field.castValue(fv_)
          val saver = field.valueSaver(fv) // TODO reuse
          val (fieldData, fieldState) = saver.save(dao, savedSteps, combinedSaveCtx, saveCtx2)
          dao.createFieldValue(fvRec, field.rec, fieldData)
          newFieldStates += (field -> fieldState)
        }

        // Link usecase to field values
        // TODO make bulk insert
        for (fv <- UCsFVs)
          dao.relate_usecase_has_fieldValue(ucRec, fv)

        // Save checkpoint
        val finalSaveCtx =
          if (removeOldFVs.isEmpty) combinedSaveCtx
          else combinedSaveCtx.copy(fieldValues = combinedSaveCtx.fieldValues.filterNot(e => removeOldFVs.contains(e._2)))
        val checkpoint = UseCaseSaveCheckpoint(uc, ucRec.value, finalSaveCtx, newFieldStates, savedSteps)

        Some(checkpoint)
      }
    }

    // Safely save
    prevSave.map(p => Locks.UseCase.withWriteLock(p.rec.dataId)(actuallySave))
    .getOrElse(actuallySave)
  }

  /**
   * When a use case is updated, sometimes the stepsAndLabels map needs to be updated, other times it can be reused.
   * This will compare two UCs and return a new UC that is guaranteed to have an up-to-date stepsAndLabels map.
   *
   * @param original The UC before the update (ie. with a correct stepsAndLabels map).
   * @param updated An updated UC with a possibly-incorrect stepsAndLabels map.
   * @return A UC with the updated UC state, and a correct stepsAndLabels map.
   */
  def correctStepsAndLabelsAfterUpdate(original: UseCase, updated: UseCase): UseCase = {

    def reusable = (original eq updated) || (ucNumbersMatch && fieldValuesReusable)

    def ucNumbersMatch = original.header.number == updated.header.number

    def fieldValuesReusable = (original.fieldValues eq updated.fieldValues) || !relevantFieldValuesDiffer

    def relevantFieldValuesDiffer = original.fields.exists {
      case f: StepField => stepFieldValuesDiffer(f(original.fieldValues), f(updated.fieldValues))
      case _ => false
    }

    def stepFieldValuesDiffer(a: StepFieldValue, b: StepFieldValue) = a.tree ne b.tree

    if (reusable) updated
    else updated.regenerateStepsAndLabels
  }
}

// =====================================================================================================================

case class UseCase(
  header: UseCaseHeader,
  fields: List[Field],
  fieldValues: FieldValues,
  stepsAndLabels: StepAndLabelBiMap) {

  assume(fieldValues.keySet == fields.toSet, "There must be a field value for all fields.")

  import UseCaseFns._

  implicit protected def stepsAndLabelsImplicit = stepsAndLabels

  def toPrettyString: String = {
    val line = "-" * 98
    val fieldsPP = fields.map(f => s"  F: $f\n  V: ${fieldValues(f)}\n").mkString("\n")
    val snl = stepsAndLabels.get.ab.map {case (id, lbl) => "  %-16s <-- %s".format(lbl, id)}.toList.sorted.mkString("\n")
    (s">$line>\n"
      + s"Header: $header\n"
      + s"Fields:\n$fieldsPP\n"
      + s"StepsAndLabels (${stepsAndLabels.get.size}):\n$snl\n"
      + s"<$line<")
    .replace(FreeText.empty.toString, "FreeText.empty")
    .replace(filter[NormalCourseField](fields).head.empty.toString, "StepFieldValue.empty")
    .replace(filter[ExceptionCourseField](fields).head.empty.toString, "StepFieldValue.empty")
  }

  def pp() = { println(toPrettyString); this }


  def regenerateStepsAndLabels: UseCase =
    copy(stepsAndLabels = generateStepAndLabelBiMap(this))

  /**
   * Passes a list of changes to all parts of a UC that respond to changes, allowing the components to transform
   * themselves based on the change. Finally a new UC with transformed state is returned.
   *
   * NOTE 1: StepsAndLabels is expected to be up-to-date.
   * NOTE 2: Input changes are will be present in the output changes. NoChange is a valid result here.
   */
  def respondToChanges(changes: NonEmptyList[Change]): UcUpdateResult = {

    def changeField(fieldValue: Field#Value): ChangeResult[Field#Value, Change] = {
      var fieldChanges = List.empty[Change]
      var fv = fieldValue
      for (c <- changes.list)
        fv.respondToChange(c) match {
          case Changed(newFv, newChanges) =>
            fv = newFv
            fieldChanges ++= newChanges.list
          case NoChange =>
        }
      ChangeResult <~ (fv, fieldChanges)
    }

    val changeAllFields = {
      var totalChanges = List.empty[(Field, Change)]
      var fvs = this.fieldValues
      for (f <- this.fields; origFv <- this.fieldValues.get(f))
        changeField(origFv) match {
          case Changed(newFv, newChanges) =>
            fvs += (f -> newFv)
            totalChanges ++= newChanges.list.map(keyByField(f))
          case NoChange =>
        }
      ChangeResult <~ (fvs, totalChanges)
    }

    changeAllFields.map(newFVs => copy(fieldValues = newFVs))
  }

  def afterRespondingToChange(change: Change): UseCase = afterRespondingToChanges(change.asOnlyChange)
  def afterRespondingToChanges(changes: NonEmptyList[Change]): UseCase = respondToChanges(changes).getOrElse(this)

  // ------------------------------------------------------------------------------------------------------------

  @inline final def update[V](f: Field, cr: ChangeResultF[V, Change])(implicit l: AppliedLens[UseCase, V]): UcUpdateResult =
    update(cr, keyByField(f) _)

  def update[V](cr: ChangeResultF[V, Change], changeMapFn: Change => (UcChangeDomain, Change))(implicit l: AppliedLens[UseCase, V]): UcUpdateResult =
    cr.flatMapF((newValue, changes) => {
      val update1 = l.set(newValue).regenerateStepsAndLabels
      val update2 = update1.respondToChanges(changes)
      addChangesToResult(update1, update2, changes.map(changeMapFn))
    })

  // TODO input-correction not sent back to client when state stays the same
  def updateTitle(input: String): UcUpdateResult = {
    implicit val lens = alens(FieldLenses.uc.title, this)
    val newTitle = InputCorrection.useCaseTitle(input)
    if (lens.get == newTitle) NoChange
    else update(newTitle @: TitleChanged(lens.get, newTitle), c => (UseCaseHeader, c))
  }
}

/** Narrows down the scope of a change. Paired with changes to indicate where (eg. which field) the change occurred. */
trait UcChangeDomain

object UseCaseHeader extends UcChangeDomain
case class UseCaseHeader(title: String, number: Short)

case class UseCaseSaveCheckpoint(
  uc: UseCase,
  rec: PlainValue[DataType.UseCase],
  saveCtx: FieldSaveCtx,
  fieldStates: FieldStates,
  savedSteps: SavedSteps)
