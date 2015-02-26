package shipreq.webapp.client.app.ui.reqtable

import scalaz.NonEmptyList
import shipreq.base.util.UnivEq

sealed abstract class SortMethod(symbol: String, desc: String) {
  val optionLabel = symbol + " " + desc
}
object SortMethod {
  // http://en.wikipedia.org/wiki/Geometric_Shapes
  private def ascSym   = "▲"
  private def descSym  = "▼"
  private def blankSym = "◌"
  private def ascTxt   = "Ascending"  // English
  private def descTxt  = "Descending" // English
  private def blankTxt = "Blanks"     // English
  @inline private def txt1(a: String) = a
  private def txt2(a: String, b: String) = a + " then " + b // English

  sealed abstract class IgnoreBlanks(symbol: String, desc: String) extends SortMethod(symbol, desc)
  case object Asc  extends IgnoreBlanks(ascSym,  txt1(ascTxt))
  case object Desc extends IgnoreBlanks(descSym, txt1(descTxt))

  sealed abstract class ConsiderBlanks(symbol: String, desc: String) extends SortMethod(symbol, desc)
  case object BlanksThenAsc  extends ConsiderBlanks(blankSym + ascSym  , txt2(blankTxt, ascTxt  ))
  case object BlanksThenDesc extends ConsiderBlanks(blankSym + descSym , txt2(blankTxt, descTxt ))
  case object AscThenBlanks  extends ConsiderBlanks(ascSym   + blankSym, txt2(ascTxt,   blankTxt))
  case object DescThenBlanks extends ConsiderBlanks(descSym  + blankSym, txt2(descTxt,  blankTxt))

  @inline implicit def equalityI: UnivEq[IgnoreBlanks] = UnivEq.force
  @inline implicit def equality : UnivEq[SortMethod]   = UnivEq.force

  // Lazy due to initialisation order. https://github.com/scala-js/scala-js/issues/1490
  lazy val ignoreBlanks   = NonEmptyList[IgnoreBlanks](Asc, Desc)
  lazy val considerBlanks = NonEmptyList[ConsiderBlanks](AscThenBlanks, BlanksThenAsc, BlanksThenDesc, DescThenBlanks)

  val valuesAllowed: Column.SortInconclusive => NonEmptyList[SortMethod] = {
    case Column.ReqType => ignoreBlanks
    case Column.Code
         | Column.Desc
         | Column.Tags
         | Column.ImplicationSrc
         | Column.ImplicationTgt
         | Column.CustomField(_) => considerBlanks
  }
}
