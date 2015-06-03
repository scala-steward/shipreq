package shipreq.webapp.client.test

import japgolly.scalajs.react._
import org.scalajs.dom.Node

object ReactTmpExt {

  // TODO Add to scalajs-react

  implicit class ASDASD_1[P, S, B, N <: TopNode](val r: ReactComponentC[P, S, B, N]) extends AnyVal {
    type Props = P
    type State = S
    type Backend = B
    type Mounted = ReactComponentM[P, S, B, N]
    type Unmounted = ReactComponentU[P, S, B, N]

//    def castM(n: Node): Mounted = n.asInstanceOf[Mounted]

    def castM(c: ReactComponentM_[_]): Mounted = c.asInstanceOf[Mounted]
    def castU(c: ReactComponentU_): Unmounted = c.asInstanceOf[Unmounted]

//    val x0 = ReactTestUtils.findRenderedComponentWithType(c, Table.Component.jsCtor)
//    val x = Table.Component.castM(x0)

  }

}
