package shipreq.webapp.client.ui
package table

import japgolly.scalajs.react._, vdom.ReactVDom._, all._, ScalazReact._
import japgolly.scalajs.react.test.{Simulation, ChangeEventData, ReactTestUtils}
import org.scalajs.dom
import scalaz.effect.IO
import scalaz.std.string.stringInstance
import scalaz.Equal
import utest._
import shipreq.webapp.shared.validation.{GenericValidators, ValidatorPlus}
import shipreq.webapp.client.protocol.FailureIO
import ValidatorPlus.Implicits._


object TableTest extends TestSuite {

  case class Data(name: String, desc: Option[String]) //, num: Int)
  implicit def DataEqual = Equal.equalA[Data]

  val nameV = ValidatorPlus.nop[String]
  val descV = GenericValidators.optionalLargeText("desc").toPlus

  val prespec = TableSpecBuilder[Data](
    FieldSpec[Data](_.name)(nameV)(Editors.TextInputEditor),
    FieldSpec[Data](_.desc)(descV)(Editors.TextareaEditor)
  ).buildU(Data).dataId[Int]

  private val prespec2 = prespec
      .tableConstraints(None, None)
      .saveNotNeededWhenP

  class Tester {
    var i = 0
  }

  override def tests = TestSuite {

    'asyncSaveFailure {

      type X = Tester
      def save(x: X, o: Option[(Int, Data)], u: Data, f: FailureIO) = IO[Unit] {
        println("yyyyyaaaaaaaaaaayyyyyyy")
        x.i += 1
      }
      val spec = prespec2.asyncSave(save)

      val refs = Ref.param[Int, TopNode](_.toString)

      val C = ReactComponentB[(X, Map[Int, Data])]("C")
        .getInitialState(p => spec.initialState(p._2))
        .render(T => {
          implicit def x = T.props._1
          val savedRow = spec.savedRow((_, d, vv) => {
            val (name, desc) = vv
            div(keyAttr := d, ref := refs(d), name, desc)
          })
          val savedRows = spec.savedRows(T, savedRow)(_.sortBy(_._3.name))
          div(savedRows)
        }).create

      val t = new Tester
      val data = Map(2 -> Data("ABC", None)) //, 3 -> Data("DEF", Some("YAG")))
      val c = ReactTestUtils renderIntoDocument C((t, data))

      val r2 = refs(2)(c).get
      println(s"R2 = $r2")
      val xx = ReactTestUtils.findRenderedDOMComponentWithTag(r2, "input").domType[dom.HTMLInputElement]
      val xxx = xx.getDOMNode()
      println(s"XX = $xx | $xxx")
      println(s"xxx.value = ${xxx.value}")

      Simulation.changeAndBlur("nice") run xx

      println(s"xxx.value = ${xxx.value}")
      println(s" TESTER = ${t.i}")

//      val data = Map(2 -> Data("ABC", None)) //, 3 -> Data("DEF", Some("YAG")))
//      println(React renderComponentToStaticMarkup Comp(data))
//
//      val c1 = Comp.apply(data)
//      val c = ReactTestUtils.renderIntoDocument(c1)
//      val r = c.asInstanceOf[js.Dynamic].refs
//      println(s"REFS: $r")
//      val r2 = c.asInstanceOf[js.Dynamic].refs.hey
//      println(s" REF: $r2")
//      val i = ReactTestUtils.findRenderedDOMComponentWithTag(c, "input")
//      println(s"I = $i")
//      ReactTestUtils.Simulate.change(i, js.Dynamic.literal(data = "no"))
//      println(s"I = $i")
    }
  }
}