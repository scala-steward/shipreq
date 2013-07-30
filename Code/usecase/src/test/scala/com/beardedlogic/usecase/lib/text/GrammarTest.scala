package com.beardedlogic.usecase
package lib.text

import org.scalatest.FunSpec
import org.scalatest.prop.{TableFor2, PropertyChecks}
import test.TestHelpers

class GrammarTest extends FunSpec with TestHelpers with PropertyChecks {
  val G = Grammar

  describe("Grammar") {
    def test[T](parser: G.Parser[T], examples: TableFor2[String, Boolean])(expect: String => T) {
      forAll(examples)((input, pass) => {
        val r = G.parseAll(parser, input)
        r.successful should be(pass)
        if (pass) r.get should be(expect(input))
      })
    }

    it("should parse StepLabel") {
      val examples = Table(("EXAMPLE", "PASS")
        , ("1.0", true)
        , ("1.0.a", true)
        , ("1.0.a.iii", true)
        , ("1.0.a.iii.1", true)
        , ("1.E.2", true)
        , ("13.E.20.ba.xiv.23", true)
        , ("13. E . 20  .ba.xiv.23", true)
        , ("1.", false)
        , (".0", false)
        , ("1", false)
        , ("1..0", false)
        , ("X1", false)
        , ("", false)
      )
      test(G.StepLabel, examples)(_.replaceAll("\\s+", ""))
    }

    it("should parse OptionallyBracedRef") {
      val examples = Table(("EXAMPLE", "PASS")
        , ("1.0", true)
        , ("1.0.a.iii.1", true)
        , ("1.E.2", true)
        , ("13.E.20.ab.xiv.23", true)
        , ("[1.0]", true)
        , ("[13.E.20.ab.xiv.23]", true)
        , ("[ 1.0 ]", true)
        , ("[ 13.E.20.ab.xiv.23 ]", true)
        , ("[1.0", false)
        , ("1.0]", false)
        , ("[[1.0]", false)
        , ("[1.0]]", false)
        , ("[[1.0]]", false)
        , ("", false)
      )
      test(G.OptionallyBracedRef, examples)(_.replaceAll("[\\s\\[\\]]+", ""))
    }

    it("should parse FlowToRefList") {
      val examples = Table(("EXAMPLE", "PASS")
        , ("1.0", true)
        , ("1.0, 1.2", true)
        , ("[1.0], [1.2]", true)
        , ("[1.0] [1.2]", true)
        , ("[1.0] 1.2", true)
        , ("1.3 [1.0]", true)
        , ("1.0, 1.2. a, 1.3, 1.1", true)
        , ("[1.0] 1.2. a, [1.3] [1.1]", true)
        , ("", false)

      )
      test(G.FlowRefList, examples) {
        _.replace("[", ",[").replace("]", "],").replaceAll("[\\s\\[\\]]+", "").split(",+").filter(_.nonEmpty).toList
      }
    }

    it("should parse TextAndFlow") {
      forAll(TextWithFlowExamples) {
        (input, expText, expRefsFrom, expRefsTo) =>
          val r = G.parseAll(G.TextAndFlow, input)
          r.successful should be(true)
          r.get._1 should be(expText)
          val flowResults = r.get._2
          flowResults.from should be(if (expRefsFrom.isEmpty) None else Some(expRefsFrom))
          flowResults.to should be(if (expRefsTo.isEmpty) None else Some(expRefsTo))
      }
    }
  }
}