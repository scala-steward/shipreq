package shipreq.webapp.client.project.app.pages.config.fields

import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.event._
import shipreq.webapp.member.test.AutoCompleteTestUtil._
import shipreq.webapp.member.test.WebappTestUtil._
import shipreq.webapp.member.test.project.SampleProject7
import shipreq.webapp.member.test.project.UnsafeTypes._
import utest._

object FormulaEditorTest extends TestSuite {

  override def tests = Tests {
    val ratingField   = 99.CFNum
    val userScoreField = 100.CFNum
    val deadNumField  = 101.CFNum

    val project = applyEventsSuccessfully(
      SampleProject7.project,

      Event.FieldCustomNumberCreate(ratingField, CustomNumberFieldGD.nev(
        CustomNumberFieldGD.Name("Rating"),
        CustomNumberFieldGD.Desc(None),
        CustomNumberFieldGD.Range((1.0, 5.0)),
        CustomNumberFieldGD.DecimalPlaces(1),
        CustomNumberFieldGD.FieldReqTypeRules(FieldReqTypeRules.optional)
      )),

      Event.FieldCustomNumberCreate(userScoreField, CustomNumberFieldGD.nev(
        CustomNumberFieldGD.Name("User Score"),
        CustomNumberFieldGD.Desc(None),
        CustomNumberFieldGD.Range((0.0, 100.0)),
        CustomNumberFieldGD.DecimalPlaces(0),
        CustomNumberFieldGD.FieldReqTypeRules(FieldReqTypeRules.optional)
      )),

      Event.FieldCustomNumberCreate(deadNumField, CustomNumberFieldGD.nev(
        CustomNumberFieldGD.Name("Dead Weight"),
        CustomNumberFieldGD.Desc(None),
        CustomNumberFieldGD.Range((0.0, 10.0)),
        CustomNumberFieldGD.DecimalPlaces(0),
        CustomNumberFieldGD.FieldReqTypeRules(FieldReqTypeRules.optional)
      )),

      Event.FieldCustomDelete(deadNumField)
    )

    implicit val strategies = FormulaEditor.autoCompleteStrategies(project.config)

    "functions" - {
      "caseInsensitivePrefix" - {
        assertSuggestionsAndSelectionFor("av")("AVERAGE(")(expectedResult = "AVERAGE(|)")
        assertSuggestionsAndSelectionFor("AV")("AVERAGE(")(expectedResult = "AVERAGE(|)")
      }

      "multipleFunctionMatches" - {
        assertSuggestionsFor("is")("ISBLANK(", "ISBOOL(", "ISERR(", "ISNUMBER(", "ISTEXT(")
      }

      "midFormula" - {
        assertSuggestionsAndSelectionFor("1 + flo")("FLOOR(")(expectedResult = "1 + FLOOR(|)")
      }
    }

    "fields" - {
      "singleWordName" - {
        assertSuggestionsAndSelectionFor("field:R")("field:Rating")(expectedResult = "field:Rating")
      }

      "multiWordNameQuoted" - {
        assertSuggestionsAndSelectionFor("field:\"u")("field:\"User Score\"")(expectedResult = "field:\"User Score\"")
      }

      "deadFieldsExcluded" - {
        assertSuggestionsFor("field:D")()
      }
    }
  }
}
