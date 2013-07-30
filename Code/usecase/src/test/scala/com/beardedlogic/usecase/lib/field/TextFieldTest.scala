package com.beardedlogic.usecase
package lib.field

import org.scalatest.FunSpec
import org.mockito.Mockito._
import lib.Types._
import lib.text.FreeText
import model._
import test.TestHelpers

class TextFieldTest extends FunSpec with TestHelpers {
  type V = FreeText
  type S = TextWithNormalisedRefs

  def parseExact(txt: String)(implicit stepsAndLabels: StepAndLabelBiMap) = {
    val v = FreeText.parse(txt)
    v.text should be(txt)
    v
  }

  describe("Field.apply()") {
    it("should lookup the field value and cast result") {
      val tf1 = freeText("1")
      val tf2 = freeText("2")
      val m: FieldValues = Map(TF2 ~> tf1, TF3 ~> tf2, NCF ~> NCF.empty)
      var r: FreeText = TF2(m)
      r should be(tf1)
      r = TF3(m)
      r should be(tf2)
    }
  }

  describe("Loading") {
    describe("load") {
      val Value_1 = new FieldValueFullRec(11, 1, 1, TF1.rec.valueId, Some("Jord"))
      val Value_2 = new FieldValueFullRec(22, 2, 1, TF2.rec.valueId, Some("puls"))
      val DbFieldValues = Map(TF1.rec.taggedId -> Value_1, TF2.rec.taggedId -> Value_2)
      val LoadCtx = new FieldLoadCtx(DbFieldValues, null, null)

      it("should return a blank string when no field value exists") {
        TF1.load(FieldLoadCtx(Map.empty, null, null), null) should be("")
      }

      it("should return the loaded field value when available") {
        TF1.load(LoadCtx, null) should be("Jord")
        TF2.load(LoadCtx, null) should be("puls")
      }
    }

    describe("denormalise") {
      it("should accept simple text") {
        val t = TF1.denormalise("Hehe!".hasNormalisedRefs, EmptySavedSteps)._2(EmptyStepAndLabelBiMap)
        t should be(FreeText("Hehe!", Map.empty))
      }

      it("should accept text with normalised refs") {
        val t = TF1.denormalise("look at [D.143]".hasNormalisedRefs, SavedSteps1)._2(StepState1)
        t should be(FreeText("look at [S.3]", Map(X3 -> S3)))
      }
    }
  }

  describe("Saving") {
    implicit def ss = StepState1

    def saver(v: V) = TF1.valueSaver(v)

    describe("record_required_?") {
      it("should not require a record when no text") {
        saver(FreeText.empty).record_required_? should be(false)
      }
      it("should require a record when text is present") {
        saver(FreeText.parse("hello")).record_required_? should be(true)
      }
    }

    def testPresave(v: V, prevSave: Option[S], expectChange: Boolean, savedSteps: SavedSteps = SavedSteps1) {
      val saveCtx = mock[MutableFieldSaveCtx]
      val dao = mock[DAO]
      val s = saver(v)
      s.record_required_? should be(true)
      s.presave(dao, prevSave.map((mock[FieldSaveCtx], _)), savedSteps)(saveCtx) should be(expectChange)
      verifyZeroInteractions(saveCtx)
      verifyZeroInteractions(dao)
    }

    describe("presave (on first save)") {
      it("should save simple text") {
        testPresave(FreeText.parse("Hello"), None, true, EmptySavedSteps)
      }
    }

    describe("presave (with a previous save)") {
      it("should save simple text when it differs") {
        testPresave(FreeText.parse("Hello!"), Some("ah".hasNormalisedRefs), true)
      }
      it("should not save simple text when unchanged") {
        testPresave(FreeText.parse("Hello!"), Some("Hello!".hasNormalisedRefs), false)
      }
      it("should not save text with refs matches unchanged, normalised text") {
        testPresave(parseExact("Hello! [S.1]"), Some("Hello! [D.141]".hasNormalisedRefs), false)
      }
      it("should save text with refs matches differs") {
        testPresave(parseExact("Hello! [S.1]"), Some("Hello! [D.222]".hasNormalisedRefs), true)
      }
    }

    describe("save") {
      def testSave(text: String, expectedSaveText: String) {
        val saveCtx = mock[MutableFieldSaveCtx]
        val dao = mock[DAO]
        val s = saver(parseExact(text))
        s.record_required_? should be(true)
        s.presave(dao, None, EmptySavedSteps)(saveCtx) should be(true)
        val r = s.save(dao, SavedSteps1, null, null)
        r._1 should be(Some(expectedSaveText))
        r._2 should be(expectedSaveText)
        verifyZeroInteractions(saveCtx)
        verifyZeroInteractions(dao)
      }

      it("should save simple text") {
        testSave("Hello", "Hello")
      }
      it("should text with normalised refs") {
        testSave("Hello [S.2]", "Hello [D.142]")
      }
    }
  }
}
