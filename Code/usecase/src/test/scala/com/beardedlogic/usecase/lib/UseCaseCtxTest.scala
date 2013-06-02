package com.beardedlogic.usecase
package lib

import net.liftweb.util.Helpers.nextFuncName
import org.scalatest.FunSpec
import scala.slick.jdbc.{StaticQuery => Q}
import Q.interpolation
import test.{TestHelpers, TestDatabaseSupport}
import field._
import TypeTags._
import model._
import NodeUtils._
import StepTree.{Step => Step2, _}

class UseCaseCtxTest extends FunSpec with TestDatabaseSupport with TestHelpers {

  implicit def autoTagLocalStepId(s: String) = s.asLocalStepId

  describe("Loading") {
    it("should load a simple, manually-saved UC") {
      // Create UC
      val uc = db.createInitialValue(DataType.UseCase)
      val uc_id = uc.valueId
      sqlu"INSERT INTO usecase VALUES(${uc_id}, 'ahh', 3, ${Defaults.FieldList.valueId})".execute

      // Create Text FV
      val txt_fk = Defaults.FieldList.fieldKeys.filter(_.fieldKeyType == FieldKeyType.Text).head
      val txt_fv = db.createInitialValue(DataType.FieldValue)
      db.createFieldValue(txt_fv, txt_fk, Some("Hehe!"))
      db.relate_usecase_has_fieldValue(uc, txt_fv)

      // Create course FV
      val c_fk = Defaults.FieldList.fieldKeys.filter(_.fieldKeyType == FieldKeyType.NormalAndAlternateCourses).head
      val c_fv = db.createInitialValue(DataType.FieldValue)
      val s1, s2 = db.createInitialValue(DataType.Step)
      db.createFieldValue(c_fv, c_fk, None)
      db.createStep(s1, "Root")
      db.createStep(s2, "Child")
      db.relate_usecase_has_fieldValue(uc, c_fv)
      db.relate_stepParent_has_step(c_fv, 0, s1)
      db.relate_stepParent_has_step(s1, 0, s2)

      // Load
      val loaded = new UseCaseCtx(null)
      val cp = UseCaseLoader.loadCheckpoint(uc_id, db)
      loaded.restoreCheckpoint(cp.get)

      // Verify
      loaded.title should be("ahh")
      loaded.number should be(3)
      loaded.fields.filter(_.fieldKey == txt_fk).head.asInstanceOf[TextField].value.text should be("Hehe!")
      loaded.ncacField.get.courses should matchTree(parseStepTree("3.0. Root\n  1. Child"))
    }

    it("should load a manually-saved UC with refs") {
      // Create UC
      val uc = db.createInitialValue(DataType.UseCase)
      val uc_id = uc.valueId
      sqlu"INSERT INTO usecase VALUES(${uc_id}, 'ahh', 3, ${Defaults.FieldList.valueId})".execute

      // Create course FV
      val c_fk = Defaults.FieldList.fieldKeys.filter(_.fieldKeyType == FieldKeyType.NormalAndAlternateCourses).head
      val c_fv = db.createInitialValue(DataType.FieldValue)
      val s1, s2, s3 = db.createInitialValue(DataType.Step)
      db.createFieldValue(c_fv, c_fk, None)
      db.createStep(s1, "Root")
      db.createStep(s2, s"Child [D.${s1.dataId}]")
      db.createStep(s3, s"Other [D.${s2.dataId}]")
      db.relate_usecase_has_fieldValue(uc, c_fv)
      db.relate_stepParent_has_step(c_fv, 0, s1)
      db.relate_stepParent_has_step(c_fv, 1, s3)
      db.relate_stepParent_has_step(s1, 0, s2)

      // Create Text FV
      val txt_fk = Defaults.FieldList.fieldKeys.filter(_.fieldKeyType == FieldKeyType.Text).head
      val txt_fv = db.createInitialValue(DataType.FieldValue)
      db.createFieldValue(txt_fv, txt_fk, Some(s"look at [D.${s2.dataId}] and [D.${s3.dataId}]!"))
      db.relate_usecase_has_fieldValue(uc, txt_fv)

      // Load
      val loaded = new UseCaseCtx(null)
      val cp = UseCaseLoader.loadCheckpoint(uc_id, db)
      loaded.restoreCheckpoint(cp.get)

      // Verify
      loaded.title should be("ahh")
      loaded.number should be(3)
      loaded.fields.filter(_.fieldKey == txt_fk).head.asInstanceOf[TextField].value.text should be("look at [3.0.1] and [3.1]!")
      val nc = loaded.ncacField.get
      nc.test__textFields(nc.courses(0).id).text should be("Root")
      nc.test__textFields(nc.courses(0)(0).id).text should be("Child [3.0]")
      nc.test__textFields(nc.courses(1).id).text should be("Other [3.0.1]")
      // loaded.ncacField.get.courses should matchTree(parseStepTree("3.0. Root\n  1. Child [3.0]\n3.1. Other [3.0.1]"))
      // TODO courses.step.text contains normalised refs. Good/bad?
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  // TODO Share sample courses. Create TestData or something.
  val NcSteps =
    StepNode(nextFuncName, 0, 0, Step2("I'm the title"), (
      StepNode(nextFuncName, 1, 1, Step2("First")) ::
        StepNode(nextFuncName, 1, 2, NewStep) ::
        StepNode(nextFuncName, 1, 3, Step2("Finally"), (
          StepNode(nextFuncName, 2, 1, Step2("Sweet")) :: Nil
          )) :: Nil
      )) :: Nil

  val EcSteps =
    StepNode(nextFuncName, 0, 1, Step2("EC 1E1"), List(StepNode(nextFuncName, 1, 1, Step2("EC 1E11")))) ::
      StepNode(nextFuncName, 0, 2, Step2("EC 1E2")) ::
      Nil

  def sampleCtx = {
    val uc = new UseCaseCtx(null)
    uc.title = "YES!"
    uc.textFields(0).value.setTextFromUser("blah")
    uc.textFields(2).value.setTextFromUser("hehe")
    uc.ncacField.get.courses = NcSteps
    uc.ecField.get.courses = EcSteps
    uc
  }

  describe("Saving (first-time)") {
    it("should set lastSave (on first save)") {
      val uc = new UseCaseCtx(null)
      uc.lastSave should be('empty)
      uc.save(db)
      uc.lastSave should not be ('empty)
    }

    it("should save when empty") {
      val uc = new UseCaseCtx(null)
      uc.courseFields.foreach(_.courses = Nil)
      assertTableDiffs("usecase" -> 1, "data" -> 1, "value" -> 1) { uc.save(db) }
    }

    it("should save with 2 text fields") {
      val uc = sampleCtx
      uc.courseFields.foreach(_.courses = Nil)
      assertTableDiffs("usecase" -> 1, "data" -> 3, "value" -> 3, "field_value" -> 2, "relation" -> 2) { uc.save(db) }
    }
  }

  describe("Updating") {
    def testUpdate(test: UseCaseCtx => Any, expectUpdate: Boolean = true) {
      val uc = sampleCtx
      uc.save(db)
      uc.lastSave should not be ('empty)
      val lastSave = uc.lastSave
      test(uc)
      uc.lastSave should (if (expectUpdate) (not be (lastSave)) else be(lastSave))
    }

    def FVs = 4
    def FVsPlus(plus: Int) = FVs + plus

    it("should do nothing when no changes") {
      testUpdate(expectUpdate = false, test = { uc =>
        assertTableDiffs() { uc.save(db) }
      })
    }

    it("should save a title change") {
      testUpdate { uc =>
        uc.title = "zzzzzzzzz"
        assertTableDiffs("usecase" -> 1, "value" -> 1, "relation" -> FVs) { uc.save(db) }
      }
    }

    it("should save a UC-number change") {
      testUpdate { uc =>
        uc.number = 666
        assertTableDiffs("usecase" -> 1, "value" -> 1, "relation" -> FVs) { uc.save(db) }
      }
    }

    it("should save a text update") {
      testUpdate { uc =>
        uc.textFields(0).value.setTextFromUser("jjjjjjjjjj")
        assertTableDiffs("usecase" -> 1, "field_value" -> 1, "value" -> 2, "relation" -> FVs) { uc.save(db) }
      }
    }

    it("should save a text removal") {
      testUpdate { uc =>
        uc.textFields(0).value.setTextFromUser("")
        assertTableDiffs("usecase" -> 1, "value" -> 1, "relation" -> FVsPlus(-1)) { uc.save(db) }
      }
    }

    it("should save a new text") {
      testUpdate { uc =>
        uc.textFields(3).value.setTextFromUser("jjjjjjjjjj")
        assertTableDiffs("usecase" -> 1, "field_value" -> 1, "value" -> 2, "data" -> 1, "relation" -> FVsPlus(1)) { uc.save(db) }
      }
    }

    it("should behave the same on updates after updates") {
      val uc = sampleCtx
      uc.save(db)
      assertTableDiffs() { uc.save(db) }
      assertTableDiffs() { uc.save(db) }

      uc.title = "zzzzzzzzz"
      assertTableDiffs("usecase" -> 1, "value" -> 1, "relation" -> FVs) { uc.save(db) }

      assertTableDiffs() { uc.save(db) }
      assertTableDiffs() { uc.save(db) }

      uc.textFields(0).value.setTextFromUser("jjjjjjjjjj")
      assertTableDiffs("usecase" -> 1, "field_value" -> 1, "value" -> 2, "relation" -> FVs) { uc.save(db) }

      assertTableDiffs() { uc.save(db) }
      assertTableDiffs() { uc.save(db) }

      uc.textFields(0).value.setTextFromUser("")
      assertTableDiffs("usecase" -> 1, "value" -> 1, "relation" -> FVsPlus(-1)) { uc.save(db) }

      assertTableDiffs() { uc.save(db) }
      assertTableDiffs() { uc.save(db) }
    }

    // TODO save when 1 step text change

    // TODO save when step order change
  }

  describe("Saving then Loading") {
    it("should load in full after saving") {
      // Save first
      val saved = sampleCtx
      val valueRows = countRowsIn("value")
      saved.save(db)
      (countRowsIn("value") - valueRows) should be > 10
      val valueId = saved.lastSave.get.uc.valueId

      // Then load back
      val loaded = new UseCaseCtx(null)
      load(loaded, valueId)
      loaded.title should be(saved.title)
      loaded.number should be(saved.number)
      loaded.textFields(0).value.text should be("blah")
      loaded.textFields(1).value.text should be("")
      loaded.textFields(2).value.text should be("hehe")
      loaded.ncacField.get.courses should matchTree(NcSteps)
      loaded.ecField.get.courses should matchTree(EcSteps)
    }
  }

  def load(ucCtx: UseCaseCtx, valueId: Long) {
    val checkpoint = UseCaseLoader.loadCheckpoint(valueId, db)
    checkpoint should not be (None)
    ucCtx.restoreCheckpoint(checkpoint.get)
  }
}