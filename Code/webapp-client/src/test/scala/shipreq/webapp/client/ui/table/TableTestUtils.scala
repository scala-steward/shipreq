package shipreq.webapp.client.ui.table

import japgolly.scalajs.react.ComponentStateFocus
import utest._

object TableTestUtils {

  case class TableAssertions[S, D, P](spec: TableSpec[_, S, D, _, P, _, _], c: ComponentStateFocus[S]) {

    val initialState = c.state

    def resetState(): Unit = c setState initialState

    def test[A](f: => A): A = {
      resetState()
      f
    }

    def assertRowValues[A](f: (RowStatus, P) => A) = new {
      def apply(m: (D, A)*): Unit = {
        val actual = spec.getSaved(c).map { case (r, d, p) => d -> f(r, p)}.toMap
        val expect = m.toMap
        assert(actual == expect)
      }
    }

    def assertRowStatuses =
      assertRowValues((r, p) => r match {
        case RowStatus.Sync => sync
        case RowStatus.Locked => locked
        case RowStatus.Failed(_) => failed
      })
  }

  val (sync,locked,failed) = ("sync","locked","failed")
}
