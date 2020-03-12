package shipreq.webapp.client.project.app.pages.config.tags

import japgolly.scalajs.react.test._
import shipreq.webapp.base.test.TestState._

object TagConfigTestDsl {
  val * = Dsl[Unit, TagConfigObs, Unit]

  val invariants: *.Invariants =
    *.emptyInvariant
}
