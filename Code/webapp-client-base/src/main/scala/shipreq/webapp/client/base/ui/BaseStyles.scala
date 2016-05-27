package shipreq.webapp.client.base.ui

import scalacss.Defaults._

class BaseStyles(implicit r: scalacss.mutable.Register) extends StyleSheet.Inline()(r) {
  import dsl._

  val projectItems = new ProjectItems
  final class ProjectItems {

    val item = style(
      display.flex,
      padding(1 em, `0`),
      &.not(_.firstChild)(
        // .ui.divided.items>.item
        borderTop(1 px, solid, rgba(34, 36, 38, .15))
      ),
      &.not(_.hover)(
        unsafeChild("a")(
          color(rgba(0, 0, 0, .85))))) // .ui.items>.item>.content>.header

    val itemLeft = style(
      flexGrow(1))

    val itemHeader = style(
      addClassNames("ui", "header"),
      marginBottom(`0`))

    // .ui.items>.item .meta
    val itemMeta = style(
      margin(0.5 em, `0`),
      fontSize(1 em),
      lineHeight(1 em),
      color(rgba(0, 0, 0, 0.6)))
  }
}
