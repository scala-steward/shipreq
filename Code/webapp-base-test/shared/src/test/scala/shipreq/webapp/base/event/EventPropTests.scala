package shipreq.webapp.base.event

import nyaya.util.NyayaUtilAnyExt
import nyaya.prop._
import nyaya.test.PropTest._
import scalaz.\/-
import scalaz.std.list.listInstance
import shipreq.base.util.univeq._
import shipreq.webapp.base.RandomData
import shipreq.webapp.base.data._
import shipreq.webapp.base.test.DataTestExt._
import utest._

object EventPropTests extends TestSuite {

  val AE = ApplyEvent.untrusted

  class Tester(p: Project) {
    val E = EvalOver(p)

    // Ensure project is valid. Only need to do this once globally and here it is.
    p assertSatisfies DataProp.project.allIncludingConfig

    val deletableStepProps =
      E.forall(p.useCaseStepsDeletable.map(_.id).toList) { id =>
        val a = DeleteUseCaseStep (id)
        val b = RestoreUseCaseStep(id)
        E.equal("DeleteUseCaseStep + RestoreUseCaseStep = id",
          actual = AE.apply1(a)(p).flatMap(AE.apply1(b)),
          expect = \/-(p))
        }

    def all = deletableStepProps
  }

  val prop = Prop.eval((p: Project) => new Tester(p).all)

  override def tests = TestSuite {
    RandomData.project.mustSatisfy(prop)(defaultPropSettings
      .setGenSize(4 `JVM|JS` 2)
      .setSampleSize(7 `JVM|JS` 2)
    )
  }
}
