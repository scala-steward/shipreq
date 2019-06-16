package shipreq.webapp.base.issue

import nyaya.gen.Gen
import scala.annotation.tailrec
import utest._
import shipreq.webapp.base.data.Project
import shipreq.webapp.base.event.{RandomEventStream, VerifiedEvent}
import shipreq.webapp.base.test.WebappTestUtil._

object IncrementalDetectionTest extends TestSuite {

  private def Reps       = 1
  private def StreamSize = 64

  private def initTrackerFromEvents(es: Seq[VerifiedEvent]): IssueTracker = {
    val initProject = applyVerifiedEventSuccessfully(Project.empty, es: _*)
    IssueTracker(initProject)
  }

  private def test(windowSize: Int): Unit =
    for (_ <- 1 to Reps)
      test(windowSize, Gen.long.sample())

  private def test(windowSize: Int, seed: Long): Unit = {

    val ves = (Gen.setSeed(seed) >> RandomEventStream.justEntireEventStream(StreamSize)).sample()

    @tailrec
    def go(it: IssueTracker, remainingEvents: Vector[VerifiedEvent], taken: Int): Unit =
      if (remainingEvents.nonEmpty) {
        val es     = remainingEvents.take(windowSize)
        val p2     = applyVerifiedEventSuccessfully(it.project, es: _*)
        val it2    = it.update(es.iterator.map(_.event), p2)
        val taken2 = taken + windowSize
        val expect = initTrackerFromEvents(ves.take(taken2))

        onFail {
          assertIssueSet(s"[windowSize=$windowSize, seed=${seed}L, events=$taken]",
            actual = it2.issues.vector.map(_.issue),
            expect = expect.issues.vector.map(_.issue))
        } {
          for (i <- 0 until taken2) {
            val e = ves(i)
            printf("%2d: %s\n", i, e.event.toString.take(160).replace('\n', ' '))
          }
        }

        go(it2, remainingEvents.drop(windowSize), taken2)
      }

    val init = initTrackerFromEvents(ves.take(windowSize))
    go(init, ves.drop(windowSize), windowSize)
  }

  override def tests = Tests {

    'prop {
      "1" - test(1)
      "2" - test(2)
      "4" - test(4)
      "8" - test(8)
    }

  }
}
