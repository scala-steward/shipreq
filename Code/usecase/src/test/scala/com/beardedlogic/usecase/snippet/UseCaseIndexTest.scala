package com.beardedlogic.usecase.snippet

import org.scalatest.FunSpec
import com.beardedlogic.usecase.test.TestDatabaseSupport
import com.beardedlogic.usecase.lib.msg.NoReaction
import com.beardedlogic.usecase.model.{UseCaseSummary, UseCase}

class UseCaseIndexTest extends FunSpec with TestDatabaseSupport {

  describe("New UC") {

    def createNewUseCase: UseCaseSummary = assertTableDiffs('data -> 1, 'value -> 1, 'usecase -> 1) {
      UseCaseIndex.createNewUseCase(NoReaction, db)
    }

    it("should create the first as '1. Untitled'") {
      val uc = createNewUseCase
      uc.number should be(1)
      uc.title should be("Untitled")
    }

    // TODO New-UC has GLOBAL scope.

    it("should create the second as '2. Untitled'") {
      createNewUseCase
      val uc = createNewUseCase
      uc.number should be(2)
      uc.title should be("Untitled")
    }
  }
}
