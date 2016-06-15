package shipreq.webapp.client.base.ui.semantic

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.univeq.UnivEq
import org.scalajs.dom.html

/** http://semantic-ui.com/modules/dropdown.html
  */
object Dropdown {

  sealed abstract class ItemState(c: ClassName) extends HasClass(c)
  object ItemState {
    case object Default  extends ItemState(NoClass)
    case object Loading  extends ItemState("loading")
    case object Error    extends ItemState("error")
    case object Active   extends ItemState("active")
    case object Disabled extends ItemState("disabled")
    implicit def univEq: UnivEq[ItemState] = UnivEq.derive
  }

  sealed abstract class Item {
    val cont: ReactTag
  }

  object Item {
    private val item = "item"
    private val divItem = divCls(item)

    case class Div(content: TagMod, state: ItemState = ItemState.Default) extends Item {
      override val cont = divItem(content) <+ state
    }

    case class Link(a: ReactTagOf[html.Anchor], state: ItemState = ItemState.Default) extends Item {
      override val cont = a.addClass(item) <+ state
    }
  }

  type Items = Seq[Item]

}