package com.beardedlogic.usecase
package model

import test.TestDatabaseSupport
import net.liftweb.util.Helpers._
import org.scalatest.FunSpec
import lib.{UCEditorState, Defaults}
import lib.field._
import lib.StepTree.{Step => Step2, _}

class FieldValueTest extends FunSpec with TestDatabaseSupport {

  def getFieldKey(t: FieldKeyType) = Defaults.FieldList.fieldKeys.find(_.fieldKeyType == t).get

  def textFieldKey = getFieldKey(FieldKeyType.Text)
  def newTextField = textFieldKey.fieldDef.newFieldInstance(new UCEditorState(null), textFieldKey).asInstanceOf[TextField]

  def ncacFieldKey = getFieldKey(FieldKeyType.NormalAndAlternateCourses)
  def newNcAcField = ncacFieldKey.fieldDef.newFieldInstance(new UCEditorState(null), ncacFieldKey).asInstanceOf[NormalAndAlternateCourseFields]

  def ecFieldKey = getFieldKey(FieldKeyType.ExceptionCourses)
  def newEcField = ecFieldKey.fieldDef.newFieldInstance(new UCEditorState(null), ecFieldKey).asInstanceOf[ExceptionCourseFields]

  describe("Text fields") {
    it("should insert when has text") {
      val tf = newTextField
      tf.value.setTextFromUser("Yay!")
      val fv = assertTableDiffs("field_value" -> 1, "value" -> 1, "data" -> 1) {
        db.createInitialFieldValue(tf :: Nil)
      }
      fv.size should be(1)
      fv.head.fieldKey should be(textFieldKey)
      fv.head.fieldData should be(Some("Yay!"))
    }

    it("should NOP when text is blank") {
      val tf = newTextField
      tf.value.setTextFromUser("")
      val fv = assertTableDiffs() { db.createInitialFieldValue(tf :: Nil) }
      fv should be('empty)
    }
  }

  describe("Course fields") {
    def testSave(f: CourseFields) = {
      f.courses =
        StepNode(nextFuncName, 0, None, 0, Step2("I'm the title"), (
          new StepNode(nextFuncName, 1, 1, Step2("First")) ::
            new StepNode(nextFuncName, 1, 2, NewStep) ::
            new StepNode(nextFuncName, 1, 3, Step2("Finally"), (
              new StepNode(nextFuncName, 2, 1, Step2("Sweet")) :: Nil
              )) :: Nil
          )) :: Nil

      val fv = assertTableDiffs("field_value" -> 1, "step" -> 5, "value" -> 6, "data" -> 6, "relation" -> 5) {
        db.createInitialFieldValue(f :: Nil)
      }
      fv.size should be(1)
      fv.head.fieldKey should be(f.fieldKey)
      fv.head.fieldData should be(None)
    }

    it("should save NC/AC steps") { testSave(newNcAcField) }
    it("should save EC steps") { testSave(newEcField) }

    it("should NOP when no steps") {
      val f = newEcField
      f.courses = Nil

      val fv = assertTableDiffs() { db.createInitialFieldValue(f :: Nil) }
      fv should be('empty)
    }
  }
}
