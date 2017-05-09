package shipreq.webapp.client.project.widgets

import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html
import scalacss.ScalaCssReact._
import japgolly.microlibs.nonempty.NonEmptyVector
import shipreq.webapp.client.base.ui.semantic.{Accordion, Modal}
import shipreq.webapp.client.project.app.Style.{help => *}

object HelpModal {
  type Groups  = NonEmptyVector[Group]
  type Group   = Accordion.Item
  type Example = VdomTagOf[html.TableRow]

  def apply(modalHeader: VdomNode, groups: Groups): Modal =
    Modal(modalHeader, Accordion.Component(groups))

  def Groups(g1: Group, gn: Group*): NonEmptyVector[Group] =
    NonEmptyVector(g1, gn.toVector)

  def Group(title: VdomNode)(e1: Example, en: Example*): Group = {
    val content = <.table(*.examplesTable, <.tbody(e1 +: en: _*))
    Accordion.Item(title, content)
  }

  def Example(desc: TagMod*)(samplePlainText: String): Example =
    <.tr(
      exampleDesc(desc: _*),
      exampleSample(samplePlainText))

  private val exampleDesc = <.td(*.exampleDesc)
  private val exampleSample = <.td(*.exampleSample)

  val code = <.code(*.exampleDescCode)
}
