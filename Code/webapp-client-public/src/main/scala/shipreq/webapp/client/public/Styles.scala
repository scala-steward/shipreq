package shipreq.webapp.client.public

import japgolly.scalajs.react.vdom.all._

/**
  * Not using ScalaCss because of how much increase it adds to the JS.
  */
object Styles {

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  object layout {

    val cont = TagMod(
      display.flex,
      flexDirection.column,
      alignItems.stretch,
      minHeight := "100%")

    val header = TagMod(
      width := "100%",
      display.flex,
      alignItems.flexStart)

    private def headerLogoEm = 4

    val headerSides = TagMod(
      width := s"${headerLogoEm + 1}em",
      padding := "0.5em")

    val headerMid = TagMod(
      flex := "1",
      textAlign.center,
      paddingTop := "0.2em")

    val headerLogo = TagMod(
      width := s"${headerLogoEm}em",
      height := s"${headerLogoEm}em",
      display.block)

    val linkSep = TagMod(
      padding := "0 1em",
      color := "#aaa")

    val linkActive = TagMod(
      fontWeight.bold,
      color := "#000")

    val footer = TagMod(
      fontSize := "0.85rem",
      paddingTop := "0.1em",
      paddingBottom := "0.1em",
      background := "#edeeef",
      borderTop := "1px solid rgba(34,36,38,.15)",
      textAlign.center)

    val footerTxt =
      color := "#888"

    val main = TagMod(
      flex := "1",
      paddingBottom := "2em")
  }

}
