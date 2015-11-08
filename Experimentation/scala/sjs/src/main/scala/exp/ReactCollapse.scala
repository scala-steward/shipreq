package exp

import japgolly.scalajs.react._

import scalajs.js

case class ReactCollapse(isOpened         : Boolean
//                                   appear       : js.UndefOr[Boolean] = js.undefined,
                                   ) {
  def toJs: js.Object = {
    val p = js.Dynamic.literal("isOpened" -> isOpened)
    p
  }

  /*
        React.createElement(
        ReactCollapse,
        { isOpened: isOpened, style: style.baseContainer },
   */

  def apply(children: ReactNode*): ReactComponentU_ = {
//    type Z = js.UndefOr[Nothing]
    type Z = Nothing
    val ReactCollapse = js.Dynamic.global.ReactCollapse.asInstanceOf[JsComponentType[js.Object, Z, TopNode]]
    val f = React.createFactory[js.Object, Z, TopNode](ReactCollapse)
    f(toJs, children: _*)

//    val ReactCollapse = js.Dynamic.global.ReactCollapse
////    js.Dynamic.global.React.createElement(ReactCollapse, toJs, children: _*).asInstanceOf[ReactComponentU_]
//    React2.createElement(ReactCollapse, toJs, children: _*)
  }
}

@js.annotation.JSName("React")
@js.native
object React2 extends js.Object {
  def createElement(comp: js.Any, props: js.Object, children: ReactNode*): ReactComponentU_ = js.native
}

object ReactCollapse {

}
