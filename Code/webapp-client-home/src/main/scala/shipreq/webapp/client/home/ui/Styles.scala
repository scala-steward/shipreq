package shipreq.webapp.client.home.ui

import scalacss.Defaults._
import shipreq.webapp.client.base.ui.BaseStyles

object Styles extends StyleSheet.Inline {
  import dsl._

  val base = new BaseStyles

  val homeContentContainer = style(
    addClassNames("ui", "container"),
    maxWidth(700 px).important,
    marginTop(5 rem))
}