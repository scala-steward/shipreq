package shipreq.webapp.server.db

import nyaya.prop._
import utest._
import shipreq.webapp.base.data.{Project, ProjectCatalogue}
import shipreq.webapp.base.event.{ActiveEvent, RandomEventStream}
import shipreq.webapp.server.db.EventDao.EventSeq
import shipreq.webapp.server.test.TestDb
import shipreq.webapp.server.test.WebappServerTestUtil._

/** Ensures that ProjectCatalogue.Item content always matches project content.
  */
object ProjectCatalogueTest extends TestSuite {

  val propProjectName =
    Prop.equal[(ProjectCatalogue.Item, Project)]("Project name")(_._2.name, _._1.name)

  val propReqCount =
    Prop.equal[(ProjectCatalogue.Item, Project)]("Req count")(_._2.reqs.size, _._1.reqCount)

  val propEventCount = Prop.equal[(ProjectCatalogue.Item, Int)]("Event count")(
    x => (x._2 - RandomEventStream.InitialEventCount) max 0,
    _._1.eventCount)

  type P = (ProjectCatalogue.Item, Project, Int)

  val prop: Prop[P] =
    (propProjectName & propReqCount).contramap[P](x => (x._1, x._2)) &
    propEventCount.contramap[P](x => (x._1, x._3))

  override def tests = TestSuite {

    TestDb.DbUtil { dbu =>
      val uid = dbu.newUserId()

      // Do this twice to ensure that other projects' events don't interfere
      for (_ <- 1 to 2) {
        val pid = dbu.newProjectId(uid)

        val ves = RandomEventStream.entireEventStream(50).samples().next()._2
        var p = Project.empty
        for (idx <- ves.indices) {
          val ve = ves(idx)

          ve.event match {
            case ae: ActiveEvent =>
              dbu.dao.createEvent(pid, EventSeq(idx), ae, ve.hashRecs)
            case x =>
              fail("Can't create non-active event: " + x)
          }

          p = applyEventSuccessfully(p, ve.event)

          val i = dbu.dao.findProjectCatalogueItem(uid, pid) getOrElse
            fail(s"ProjectCatalogueItem not found for ($uid,$pid).")

          prop.assert(i, p, idx + 1)
        }
      }
    }

  }
}
