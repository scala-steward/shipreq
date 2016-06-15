package shipreq.webapp.client.home.ui

import scalacss.Defaults._
import shipreq.webapp.client.base.ui.BaseStyles

object Styles extends StyleSheet.Inline {
  import dsl._

  val createProjectContainer = style(
    marginBottom(BaseStyles.projectItems.vspace))
}