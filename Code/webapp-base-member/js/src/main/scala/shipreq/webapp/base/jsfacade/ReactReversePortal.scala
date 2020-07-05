package shipreq.webapp.base.jsfacade

import japgolly.scalajs.react._
import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation._

object ReactReversePortal {

  @JSGlobal("RRP")
  @js.native
  @nowarn
  object Instance extends js.Object {

    def createHtmlPortalNode(): Node      = js.native
    val InPortal              : js.Object = js.native
    val OutPortal             : js.Object = js.native
  }

  sealed trait Node extends js.Any

  sealed trait Props extends js.Object {
    val node: Node
  }

  object Props {
    def apply(n: Node): Props =
      new Props {
        override val node = n
      }
  }

  val InPortal  = JsComponent[Props, Children.Varargs, Null](Instance.InPortal)
  val OutPortal = JsComponent[Props, Children.None   , Null](Instance.OutPortal)
}
