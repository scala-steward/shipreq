package shipreq.webapp.client.home.ui

import scalacss.Defaults._
import shipreq.webapp.client.base.ui.BaseStyles

object Styles extends StyleSheet.Inline {
  import dsl._

  val base = new BaseStyles

  val homeContentContainer = style(
    maxWidth(700 px),
    margin.horizontal(auto),
    marginTop(5 rem))

  val createProjectContainer = style(
    marginBottom(base.projectItems.vspace))
}