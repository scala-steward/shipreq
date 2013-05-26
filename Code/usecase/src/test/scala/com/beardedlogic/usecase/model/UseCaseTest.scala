package com.beardedlogic.usecase
package model

import test.TestDatabaseSupport
import org.scalatest.FunSpec
import lib.UCEditorState
import lib.field._

class UseCaseTest extends FunSpec with TestDatabaseSupport {

  it("should save when empty") {
    val uce = new UCEditorState(null)
    uce.courseFields.foreach(_.courses = Nil)
    assertTableDiffs("usecase" -> 1, "data" -> 1, "value" -> 1) {
      db.createInitialUseCase(uce)
    }
  }

  it("should save with 2 text fields"){
    val uce = new UCEditorState(null)
    uce.courseFields.foreach(_.courses = Nil)
    val textFields = uce.fields.collect { case f: TextField => f }
    textFields.take(2).foreach(_.value.setTextFromUser("blah"))
    assertTableDiffs("usecase" -> 1, "data" -> 3, "value" -> 3, "field_value" -> 2, "relation" -> 2) {
      db.createInitialUseCase(uce)
    }
  }
}
