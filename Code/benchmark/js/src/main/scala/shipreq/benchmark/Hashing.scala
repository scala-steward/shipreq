package shipreq.benchmark

import japgolly.scalajs.benchmark._, gui._
import shipreq.webapp.base.data.Project
import shipreq.webapp.base.feature.hash.HashLogic
import shipreq.webapp.base.hash._

object Hashing {

  val p = data.project_100
  val recs = HashSchemes.latest.changes(Project.empty, p)

  val suite = GuiSuite(
    Suite("Hashing")(
      projectBM("Hash full")(HashSchemes.latest.hash),
      projectBM("Changes")(HashSchemes.latest.changes(Project.empty, _)),
      projectBM("Validate")(HashLogic.validate(recs, Project.empty, _)),
    )
  )
}