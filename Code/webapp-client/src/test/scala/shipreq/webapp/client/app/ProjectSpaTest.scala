package shipreq.webapp.client.app

import japgolly.scalajs.react.test._
import teststate._
import utest._
import shipreq.webapp.base.event.{Delete, DeleteCustomField}
import shipreq.webapp.base.test._
import shipreq.webapp.client.app.reqtable.{ReqTableTestDsl => RT}
import shipreq.webapp.client.test._
import ProjectSpaMain.{Page, Props}
import SampleProject.Values.priField

object ProjectSpaTest extends TestSuite {
  import ProjectSpaTestDsl._

  PrepareEnv()

  def runTest(action: *.Action) = {
    val cp   = new TestClientProtocol
    val cd   = TestClientData(SampleProject3.project)
    val spa  = new ProjectSpaMain(MockRemotes.projectSPA, cp, cd)
    val rc   = MockRouterCtl[Page]()
    val init = TestState(Page.ReqTable, cd.project())

    ComponentTester(spa.Component)(Props(init.page, rc)) { tester =>
      val tt  = Test(action, invariants).observe(_.observe())
      val h   = tt.run(init, Ref(cd, tester))
      // println(h.format(History.Options.colored.alwaysShowChildren))
      h.assert(History.Options.colored)
      // println(h.format(History.Options.colored))
    }
  }

  def reqTableAfterLocalConfigUpdate: *.Action = (
    testReqTable(RT.showHideColumn("Priority") >> RT.sortBy("Priority"))
      >> setPage(Page.CfgFields)
      >> applyEvents("Delete Priority field", DeleteCustomField(priField, Delete))
      >> setPage(Page.ReqTable)
      >> testReqTable()
  )

  override def tests = TestSuite {
    'reqTableAfterLocalConfigUpdate - runTest(reqTableAfterLocalConfigUpdate)
  }
}
