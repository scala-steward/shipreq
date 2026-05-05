package shipreq.webapp.client.project.app.pages.admin.access

import shipreq.webapp.base.test.TestState._

object AccessPageTestDsl {
  val * = Dsl[Unit, AccessPageObs, Unit]

  val invariants: *.Invariants =
    *.emptyInvariant
}
