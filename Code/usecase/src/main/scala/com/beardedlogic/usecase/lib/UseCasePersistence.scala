package com.beardedlogic.usecase
package lib

import Types._
import field._
import model._
import util.BiMap
import text.ParsingUtils._
import UseCaseFns._
import Misc.AnyExt

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

    // Phase 1
    val (fieldValueFns, stepsAndLabels) = {
      var stepAndLabelMaps = List.empty[Map[LocalIdStr, LabelStr]]
      val fieldValueFns = Map.newBuilder[Field, StepAndLabelBiMap => Field#Value]
      for ((f, s) <- fieldStates) {
        val (stepTreeOp, fn) = f.denormalise(f.castState(s), savedSteps)
        stepTreeOp.map(stepAndLabelMaps :+= generateStepAndLabelMap(f, _, ucRec.header))
        fieldValueFns += (f -> fn)
      }
      (fieldValueFns.result, generateStepAndLabelBiMap(stepAndLabelMaps))
    }

    // Phase 2
    val fieldValues = for ((f, fn) <- fieldValueFns) yield (f -> fn(stepsAndLabels))

    // Final results
    val uc = UseCase(ucRec.header, fieldList, fieldValues, stepsAndLabels)
    UseCaseSaveCheckpoint(uc, ucRec.value, saveCtx, fieldStates, savedSteps)
  }

  // ===================================================================================================================

  /**
   * Saves the use case.
   *
   * Does nothing if there are differences between the current UC, and the last-saved revision.
   *
   * @return A checkpoint is there was anything to save, else `None` if UC was already up-to-date.
   */
  def save(uc: UseCase, prevSave: Option[UseCaseSaveCheckpoint], dao: DAO): Option[UseCaseSaveCheckpoint] = {
    type TopLevelRelations = Set[Value[DataType.FieldValue]]
    type ObsoleteFVs = Set[PlainValue[DataType.FieldValue]]

    def presave() : Option[(FieldSaveCtx, ObsoleteFVs, TopLevelRelations)] = {
      var changesDetected = false
      val saveCtx = new MutableFieldSaveCtx
      var topLvlRels: TopLevelRelations = Set.empty
      var obsoleteFVs: ObsoleteFVs = Set.empty
      val oldSavedSteps = prevSave.map(_.savedSteps).getOrElse(BiMap.empty)

      // Check fields for changes and presave
      for ((field, fv_) <- uc.fieldValues) {
        val fv = field.castValue(fv_)
        val saver = field.valueSaver(fv)
        val fkrec = field.rec
        val oldFV: Option[PlainValue[DataType.FieldValue]] = prevSave.flatMap(_.saveCtx.fieldValues.get(fkrec))
        trace(s"$field - fkrec=$fkrec, oldFV=$oldFV")

        // Check if field has anything to save
        if (!saver.record_required_?) {
          if (oldFV.isDefined) {
            changesDetected = true
            obsoleteFVs += oldFV.get
            trace(s"$field - Nothing to save anymore. Used to be, thus removal required.")
          }
        } else {
          // Compare state and presave
          val previous = for {
            ls <- prevSave
            fs <- ls.fieldStates.get(field)
          } yield (ls.saveCtx, field.castState(fs))
          val fieldChanged = saver.presave(dao, previous, oldSavedSteps)(saveCtx)
          if (fieldChanged) {
            // Field changed, presave a new field value
            val newValue = if (oldFV.isEmpty)
              dao.createInitialValue(DataType.FieldValue)
            else
              dao.createValue(oldFV.get, LatestRev)
            trace(s"$field - New value created: rev=${newValue.rev}, id=${newValue.valueId}")
            saveCtx.fieldValues += (fkrec -> newValue)
            changesDetected = true
            topLvlRels += newValue
          } else {
            // Reuse the existing field value
            oldFV.foreach(topLvlRels += _)
            trace(s"$field - Reuse.")
          }
        }
      }

      // Check for changes to the use case itself
      changesDetected ||= prevSave.map(_.uc.header != uc.header).getOrElse(true)

      if (changesDetected) Some(saveCtx.immutable, obsoleteFVs, topLvlRels)
      else None
    }

    def save(newSaveCtx: FieldSaveCtx, obsoleteFVs: ObsoleteFVs, topLvlRels: TopLevelRelations): UseCaseSaveCheckpoint = {
      // Prepare data
      val combinedSaveCtx = newSaveCtx.modIf(prevSave.nonEmpty)(_.combineWith(prevSave.get.saveCtx))
      val savedSteps = generateSavedSteps(combinedSaveCtx)

      // Save UseCase
      val ucRec = saveHeader()
      val newFieldStates = saveNewFieldValues(savedSteps, combinedSaveCtx, newSaveCtx)
      saveRelations(ucRec, topLvlRels)

      // Create checkpoint
      val finalSaveCtx = combinedSaveCtx.modIf(obsoleteFVs.nonEmpty)(
        x => x.copy(fieldValues = x.fieldValues.filterNot(e => obsoleteFVs.contains(e._2)))
      )
      UseCaseSaveCheckpoint(uc, ucRec.value, finalSaveCtx, newFieldStates, savedSteps)
    }

    def saveHeader() : UseCaseRec = {
      val ucValue = if (prevSave.isEmpty)
        dao.createInitialValue(DataType.UseCase)
      else
        dao.createValue(prevSave.get.rec, LatestRev)
      // TODO using default FieldList
      dao.createUseCase(ucValue, uc.header, Defaults.FieldList.get)
    }

    def saveNewFieldValues(savedSteps: SavedSteps, combinedSaveCtx: FieldSaveCtx, newSaveCtx: FieldSaveCtx) : FieldStates = {
      var newFieldStates = prevSave.map(_.fieldStates).getOrElse(Map.empty)
      for {
        (field, fv_) <- uc.fieldValues
        fvRec <- newSaveCtx.fieldValues.get(field.rec)
      } {
        val fv = field.castValue(fv_)
        val saver = field.valueSaver(fv)
        val (fieldData, fieldState) = saver.save(dao, savedSteps, combinedSaveCtx, newSaveCtx)
        dao.createFieldValue(fvRec, field.rec, fieldData)
        newFieldStates += (field -> fieldState)
      }
      newFieldStates
    }

    def saveRelations(ucRec: UseCaseRec, topLvlRels: TopLevelRelations): Unit = {
      // TODO make bulk insert
      for (fv <- topLvlRels)
        dao.relate_usecase_has_fieldValue(ucRec, fv)
    }

    def perform(): Option[UseCaseSaveCheckpoint] = dao.withTransaction {
      for ((saveCtx, obsoleteFVs, topLvlRels) <- presave)
      yield save(saveCtx, obsoleteFVs, topLvlRels)
    }

    // Safely save
    prevSave.map(p => Locks.UseCase.withWriteLock(p.rec.dataId)(perform))
    .getOrElse(perform)
  }
}
