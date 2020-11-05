package shipreq.webapp.member.data

import java.time.Instant
import shipreq.webapp.member.event.RandomEventStream
import shipreq.webapp.member.test.WebappTestUtil._
import utest._

object ProjectMetaDataTest extends TestSuite {

  override def tests = Tests {

    "applyEvent" - {
      val (_, vesInit, ves) = RandomEventStream.entireEventStream(100).samples().next()
      var p = applyVerifiedEventSuccessfully(Project.empty, vesInit: _*)
      var md = looseProjectMetaData(p, eventsTotal = vesInit.length)
      val now = Instant.now()
      for (ve <- ves) {
        p = applyEventSuccessfully(p, ve.event)
        md = md.applyEvent(ve, p, now)
        md.assertInSyncWith(p)
      }
    }

  }
}
