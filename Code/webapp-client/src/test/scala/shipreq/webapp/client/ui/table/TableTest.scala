package shipreq.webapp.client.ui
package table

import japgolly.scalajs.react._, vdom.ReactVDom._, all._, ScalazReact._
import japgolly.scalajs.react.test.{Simulation, ReactTestUtils}
import org.scalajs.dom
import scalaz.effect.IO
import scalaz.std.string.stringInstance
import scalaz.Equal
import utest._
import shipreq.webapp.shared.validation.{GenericValidators, ValidatorPlus}
import shipreq.webapp.client.protocol.FailureIO
import ValidatorPlus.Implicits._
import TableTestUtils._

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
    var fs = List.empty[FailureIO]
  }

  type X = Tester
  def save(x: X, o: Option[(Int, Data)], u: Data, f: FailureIO) = IO[Unit] {
    x.fs ::= f
  }
  val spec = prespec2.asyncSave(save)

  val refs = Ref.param[Int, TopNode](_.toString)

  val C = ReactComponentB[(X, Map[Int, Data])]("C")
    .getInitialState(p => spec.initialState(p._2))
    .render(T => {
      implicit def x = T.props._1
      val savedRow = spec.savedRow((_, d, _, vv) => {
        val (name, desc) = vv
        div(keyAttr := d, ref := refs(d), name, desc)
      })
      val savedRows = spec.savedRows(T, savedRow)(_.sortBy(_._3.name))
      div(savedRows)
    }).build

  val data = Map(2 -> Data("ABC", None), 3 -> Data("DEF", Some("YAG")))
  val t = new Tester
  val c = ReactTestUtils renderIntoDocument C((t, data))
  val ta = TableAssertions(spec, c)
  import ta._

  val List(i2, i3) = List(2, 3).map(i =>
    ReactTestUtils.findRenderedDOMComponentWithTag(refs(i)(c).get, "input").domType[dom.HTMLInputElement])

  val simChange = Simulation.focusChangeBlur("x")

  override def tests = TestSuite {
    ta.resetState()
    t.fs = Nil

    'asyncSaveFailure {
      simChange run i2; assertRowStatuses(2 -> locked, 3 -> sync)
      simChange run i3; assertRowStatuses(2 -> locked, 3 -> locked)

      'inOrder {
        val List(f3, f2) = t.fs
        f2.io.unsafePerformIO(); assertRowStatuses(2 -> failed, 3 -> locked)
        f3.io.unsafePerformIO(); assertRowStatuses(2 -> failed, 3 -> failed)
      }

      'outOfOrder{
        val List(f3, f2) = t.fs
        f3.io.unsafePerformIO(); assertRowStatuses(2 -> locked, 3 -> failed)
        f2.io.unsafePerformIO(); assertRowStatuses(2 -> failed, 3 -> failed)
      }
    }
  }
}