package shipreq.webapp.client.app
package ui

import japgolly.scalacss.Defaults._
import japgolly.scalacss.ScalaCssReact._
import japgolly.scalacss.StyleS
import shipreq.webapp.base.AppConsts
import shipreq.webapp.base.data.{Alive, Dead}
import shipreq.webapp.client.lib.ConsoleIO
import scalaz.syntax.equal._

object Style extends StyleSheet.Inline {
  import dsl._

  object Missing {
    import japgolly.scalacss._
    import DslBase.ToStyle

    def styleIf(b: Boolean)(t: ToStyle*)(implicit c: Compose): StyleS =
      if (b) styleS(t: _*)(c) else StyleS.empty
  }

  import Missing._

  import japgolly.scalacss.Color

//  private implicit class boolext(val b: Boolean) extends AnyVal {
//    import japgolly.scalacss._
//    import DslBase.ToStyle
//
//    def styleIf(t: ToStyle*)(implicit c: Compose): StyleS =
//      if (b) styleS(t: _*) else StyleS.empty
//
//    @inline def styleIfNot(t: ToStyle*)(implicit c: Compose): StyleS =
//      (!b).styleIf(t: _*)
//  }

   // ==================================================================================================================

  val dragHnd = style(
    color("#000"))

  val aliveDomain = Domain.ofValues[Alive](Alive, Dead)
  private def aliveStyle(f: Alive => StyleS): Alive => StyleA =
    styleF(aliveDomain)(f)

  object reqtable {
    import ui.reqtable.Column

    object sortingSettings {

      val row = boolStyle(on => styleS(
//        styleIf(!on)(
//          backgroundColor("#e2e2e2")),
        marginBottom(0.7 ex),
        paddingRight(1 ex)))

      def dragHnd = Style.dragHnd

      val dirSelect = style(
        width(28 ex))

      val field = boolStyle(on => styleS(
        marginLeft(1 ex),
        styleIf(!on)(
          //textDecoration := ^.lineThrough,
          color("#999"))))

      // ↑ inconclusive | conclusive ↓

      val conclusiveDir = style(
        marginLeft(4 ex))

      val conclusiveField = style(
        marginLeft(1 ex))
    }

    object columnSettings { // TODO This is crazy...
      import OrderedSubsetEditor.Styles

      val row = style()

      val prop = (on: Boolean) => Styles(row, dragHnd = sortingSettings.dragHnd, label = sortingSettings.field(on))
    }

    // http://stackoverflow.com/questions/446624/table-cell-widths-fixing-width-wrapping-truncating-long-words
    val table = style(
      marginTop(1.6 ex),
      width(100 %%))

    val mnemonicLen = AppConsts.reqTypeMnemonicLength.max

    val columnPubid   = style(maxWidth((mnemonicLen + 5).ex))
    val columnReqType = style(maxWidth(mnemonicLen.ex))
//    val columnPubid   = style(maxWidth.fitContent)
//    val columnReqType = style(maxWidth.fitContent)

    val `N/A` = style(
      marginLeft.auto,
      marginRight.auto
    )

    val cell = boolStyle(focus => styleS(
      border(1 px, solid, if (focus) Color("#006") else Color("#000")),
      padding(horizontal = 2.px, vertical = 4.px), // TODO wrong way around in scalacss
      styleIf(focus)(
        backgroundColor("#e9e9ff"),
        outline(rgba(0, 0, 140, 0.15), 2 px, solid),
        outlineOffset(-1 px)
      )
    ))

  } // reqtable

  object widgets {

    val hasError = styleS(
      color("#c00"),
      backgroundColor("#fee")
    )

    val hoverShowsInfo = cursor.help

    val dead = styleS(
      textDecoration := ^.lineThrough,
      hasError
    )

    val tag = style(
      addClassName("label label-default"),
      hoverShowsInfo,
      marginRight(1 ex))

    val issue = style(hasError)

    val issueDesc = style(
      paddingLeft (0.7 ex),
      paddingRight(0.7 ex))

    // TODO Has color conflict
    val reqRef = aliveStyle(a => styleS(
      display.inlineBlock,
      color("#2363A1"),
      styleIf(a ≟ Dead)(dead),
      hoverShowsInfo))
  }

  def damnit(a: StyleA*) = ()
  damnit(
    reqtable.sortingSettings.conclusiveDir,
    reqtable.columnSettings.row,
    reqtable.table,
    widgets.tag)
//  ConsoleIO(_.log(render[String])).unsafePerformIO()
  ConsoleIO(_.info(s"Styles: ${Style.register.styles.length}")).unsafePerformIO()
}
