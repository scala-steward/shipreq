package shipreq.webapp.client.test

import shipreq.base.util.{DebugImplicits, IsoBool}

object TestState extends teststate.Exports with DebugImplicits {

  implicit val showTestReq: Show[TestClientProtocol.Req] =
    Show(i => s"${i.r.fn}: ${i.input}")

  implicit def equalByScalazEqual[A](implicit e: scalaz.Equal[A]): Equal[A] =
    Equal(e.equal)

  implicit def showIsoBool[B <: IsoBool[B]]: Show[B] =
    Show.byToString
}
