package shipreq.webapp.base.protocol.json.v1

import io.circe._
import io.circe.syntax._
import utest._
import shipreq.webapp.base.event.EventEquality._
import shipreq.webapp.base.event.RandomEventStream
import shipreq.webapp.base.protocol.json.JsonTestUtil._

object JsonProtocolTest extends TestSuite {
  import Events._

  override def tests = Tests {

    for (e <- RandomEventStream.sampleEventStreamWithProjects.iterator.map(_._1.event)) {
      assertRoundTrip(e)
    }

  }
}
