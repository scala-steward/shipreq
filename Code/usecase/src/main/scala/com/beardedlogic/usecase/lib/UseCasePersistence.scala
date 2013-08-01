package com.beardedlogic.usecase
package lib

import Types._
import field._
import model._
import util.BiMap
import text.ParsingUtils._
import UseCaseFns._

case class UseCaseSaveCheckpoint(
  uc: UseCase,
  rec: PlainValue[DataType.UseCase],
  saveCtx: FieldSaveCtx,
  fieldStates: FieldStates,
  savedSteps: SavedSteps)

object UseCasePersistence {

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
}
