package shipreq.webapp.base

import japgolly.nyaya._
import japgolly.nyaya.test.PropTest._
import scalaz.std.stream._
import utest._
import shipreq.base.util.{UnivEq, Must}
import UnivEq.{set => seqEq}
import shipreq.webapp.base.data._

object TransitiveClosureTest extends TestSuite {

  implicit def mustEquality[A: UnivEq] = UnivEq.force[Must[A]]

  case class Tester(tt: TagTree) {
    val E  = EvalOver(this)
    val tc = TransitiveClosure.auto(tt.vstream(_.id))(tt(_).fold(sys.error, _.children))

    def test =
      E.forall(tt.values.toStream) { t =>
        val r = tc(t.id)
        E.equal("Same results", Must.Exists(r), t.transitiveChildren(tt)) ∧
        E.equal("Non-reflexive", tc.nonRefl(t.id), r - t.id)
      }
  }

  def gen = RandomData.tagTree.map(Tester)

  override def tests = TestSuite {
    gen.mustSatisfyE(_.test)
  }
}
