package com.beardedlogic.usecase.model

import com.beardedlogic.usecase.lib.db.DB
import org.scalatest.fixture.FunSpec
import org.scalatest.matchers.ShouldMatchers
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.session.Session
import com.beardedlogic.usecase.lib.field.TextFieldDef
import Q.interpolation

class FieldListTest extends FunSpec with ShouldMatchers {

  type FixtureParam = Session

  override protected def withFixture(test: OneArgTest) = {
    //DB.wipe_!.init
    DB.Slick.withTransaction { implicit db: Session =>
      try withFixture(test.toNoArgTest(db))
      finally db.rollback()
    }
  }

  describe("FieldList") {

    it("should save") { implicit db: Session =>

      val f1 = TextFieldDef("Opeth", None)
      val f2 = TextFieldDef("Heritage", None)
      val f3 = TextFieldDef("Haxprocess", None)
      val fieldList = f1 :: f2 :: f3 :: Nil

      val saved =
      assertTableDiffs("data"->4, "value" -> 4, "relation" -> 3) {
        FieldList.save(fieldList)
      }

      FieldList.load(saved.id) should be (fieldList)

    }
  }

  def assertTableDiffs[T](expectations: (String,Int)*)(block : => T)(implicit db: Session) = {
    def count = expectations.map{case (t,_) => (t -> countRowsIn(t)) }.toMap
    val before = count
    val expected = expectations.map{case (t,delta) => (t, delta + before(t)) }.toMap
    val result = block
    val after = count
    after should be(expected)
    result
  }

  def countRowsIn(table: String)(implicit db: Session) =
    Q.queryNA[Int](s"select count(*) from $table").first
}