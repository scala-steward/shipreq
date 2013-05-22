package com.beardedlogic.usecase.model

import org.scalatest.fixture.FunSpec
import org.scalatest.matchers.ShouldMatchers
import scala.slick.session.Session
import com.beardedlogic.usecase.lib.db.DB

class DataTest extends FunSpec with ShouldMatchers {

  type FixtureParam = Session

  override protected def withFixture(test: OneArgTest) = {
    DB.Slick.withTransaction { implicit db: Session =>
      try withFixture(test.toNoArgTest(db))
      finally db.rollback()
    }
  }

  describe("Data") {

    it("should insert and read back") { implicit db: Session =>
      val id1 = Data.create(DataType.UseCase).id
      val id2 = Data.create(DataType.FieldList).id
      Data.find(id1) should be(Data(id1, DataType.UseCase): Data[_])
      Data.find(id2) should be(Data(id2, DataType.FieldList): Data[_])
    }
  }
}