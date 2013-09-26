package com.beardedlogic.usecase
package db

import org.scalatest.FunSpec
import test.TestDatabaseSupport
import slick.jdbc.StaticQuery.interpolation
import lib.Defaults
import lib.Types._
import lib.field.{TextFieldDefinition, NormalCourseFieldDefinition, ExceptionCourseFieldDefinition}

class DaoTest extends FunSpec with TestDatabaseSupport {

  describe("FieldList") {
    lazy val fl1 =
      NormalCourseFieldDefinition ::
        TextFieldDefinition("Opeth") ::
        TextFieldDefinition("Heritage") ::
        TextFieldDefinition("Haxprocess") ::
        ExceptionCourseFieldDefinition ::
        Nil

    lazy val fl2 =
      TextFieldDefinition("Opeth") ::
        TextFieldDefinition("Heritage") ::
        TextFieldDefinition("CHANGED") ::
        NormalCourseFieldDefinition ::
        ExceptionCourseFieldDefinition ::
        Nil

    describe("syncFieldList") {
      it("should save when never saved before") {
        sqlu" UPDATE field_key SET data='hehe' WHERE data IS NULL ".execute
        val fl = assertTableDiffs(Tables.FieldKey -> fl1.size) {dao.syncFieldList(fl1)}
        fl.fieldDefns ==== fl1
      }

      it("should use existing when already saved") {
        val save1 = dao.syncFieldList(fl1)
        assertTableDiffs() {
          val save2 = dao.syncFieldList(fl1)
          save2 ==== save1
        }
      }

      it("should save differences when it differs") {
        val save1 = dao.syncFieldList(fl1)
        val save2 = assertTableDiffs(Tables.FieldKey -> 1) {dao.syncFieldList(fl2)}
        save2.fieldDefns ==== fl2
      }
    }
  }

  // ===================================================================================================================

  describe("UseCase") {
    implicit def str2uch(title: String): UseCaseHeader = UseCaseHeader(title)

    describe("findUseCase") {
      it("should load when found") {
        val saved = dao.createUseCaseIdentAndRev1(newProjectId(), "ah")
        dao.findUseCaseRev(saved).get ==== saved
      }
    }

    describe("updateUseCaseHeader") {
      import UseCaseHeaderUpdateResult._

      def assertUC(revId: UseCaseRevId, expected: UseCaseRev, revOffset: Int) {
        val uc = dao.findUseCaseRev(revId).get
        uc.identId ==== expected.identId
        uc.rev.toInt ==== expected.rev + revOffset
        uc.header ==== expected.header
      }

      def assertAuditedUpdate(src: UseCaseRev, relationRows: Int = 0): UseCaseRev = {
        import Tables._
        assertTableDiffs(UsecaseRev -> 1, UcField -> relationRows) {
          val r = dao.updateUseCaseHeader(src, _.copy(title = "omg"))
          r match {
            case NewRevision(n) => assertUC(n, src.withTitle("omg"), 1); n
            case _ => fail("Expected NewRevision. Got " + r)
          }
        }
      }

      def assertNOP(uc: UseCaseRev, expected: UseCaseRev) {
        val r = assertTableDiffs() {dao.updateUseCaseHeader(uc, h => h)}
        r ==== AlreadyUpToDate(expected)
      }

      def createTwoRevs(projectId: ProjectId = newProjectId()) = {
        val rev1 = dao.createUseCaseIdentAndRev1(projectId, "Haha")
        val rev2s = dao.updateUseCaseHeader(rev1, _.copy(title = "wow")) match {
          case NewRevision(x) => x
          case _ => fail("Expected NewRevision.")
        }
        val rev2 = dao.findUseCaseRev(rev2s).get
        (rev1, rev2)
      }

      it("should do a direct update when rev #1 and title default") {
        val rev1 = dao.createUseCaseIdentAndRev1(newProjectId(), Defaults.useCaseHeader)
        val tgt = rev1.withTitle("omg")
        val r = assertTableDiffs() {dao.updateUseCaseHeader(rev1, _ => tgt.header)}
        r ==== DirectUpdate(tgt)
      }

      it("should do an audited update when rev #1 and non-default title changes") {
        assertAuditedUpdate(dao.createUseCaseIdentAndRev1(newProjectId(), "Haha"))
      }

      it("should do an audited update when rev #2+") {
        val (_, rev2) = createTwoRevs()
        assertAuditedUpdate(rev2)
      }

      it("should copy relationships when performing an audited update") {
        val fk1 = dao.createFieldKey(FieldKeyType.Text, Some("THE OCEAN"))
        val fk2 = dao.createFieldKey(FieldKeyType.Text, Some("PELAGIAL"))
        val uc1 = dao.createUseCaseIdentAndRev1(newProjectId(), "Haha")
        val txt1 = dao.createTextRev(dao.createTextIdent(uc1, fk1), 1, "mesopelagic".hasNormalisedRefs)
        val txt2 = dao.createTextRev(dao.createTextIdent(uc1, fk2), 1, "bathyalpelagic".hasNormalisedRefs)
        dao.linkUcToText(uc1, txt1)
        dao.linkUcToText(uc1, txt2)

        val uc2 = assertAuditedUpdate(uc1, 2)
        dao.findAllUcFieldData(uc2).map(_.textRev).map(_.toString).sorted ==== List(txt1, txt2).map(_.toString).sorted
      }

      it("should do nothing when rev #1 and no change") {
        val rev1 = dao.createUseCaseIdentAndRev1(newProjectId(), Defaults.useCaseHeader)
        assertNOP(rev1, rev1)
        assertNOP(rev1.withTitle(""), rev1)
      }

      it("should do nothing when rev #2 and no change") {
        val (_, rev2) = createTwoRevs()
        assertNOP(rev2, rev2)
        assertNOP(rev2.withTitle(rev2.header.title + "  "), rev2)
      }
    }
  }
}