package shipreq.webapp.base.test

import shipreq.webapp.base.data._
import shipreq.webapp.base.event._
import shipreq.webapp.base.text._
import UnsafeTypes._
import SampleProject4.Values.uc1
import SampleProject5.{project => project0}
import StaticField.{NormalAltStepTree => NA, ExceptionStepTree => E}

/**
 * Builds on SampleProject #5 to add:
 *   - UC-2: live UC with some dead steps
 */
object SampleProject6 {

  type     Values = SampleProject5.Values
  lazy val Values = SampleProject5.Values

  private def newTitle: Text.UseCaseTitle.OptionalText = {
    import Text.UseCaseTitle._
    Vector(
      UseCaseStepRef(16),
      Literal(" and "),
      UseCaseStepRef(17),
      Literal(" are dead. "),
      UseCaseStepRef(12),
      Literal(" and "),
      UseCaseStepRef(15),
      Literal(" are not."))
  }

  lazy val project = WebappTestUtil.applyEventsSuccessfully(project0
    , AddUseCaseStep(16, uc1, NA, "0.0".ploc) // becomes UC-n.0.2, followed by n.0.3, n.0.4.
    , DeleteUseCaseStep(16)                   // becomes UC-n.0.X.1, now looks the same as before live
    , AddUseCaseStep(17, uc1, E, ∅)           // becomes UC-n.E.1
    , DeleteUseCaseStep(17)                   // becomes UC-n.E.X.1
    , SetUseCaseTitle(uc1, newTitle)
    )

  lazy val plainText  = PlainText(project, ProjectText.Context.None)
  lazy val textSearch = TextSearch(project, plainText)
}
