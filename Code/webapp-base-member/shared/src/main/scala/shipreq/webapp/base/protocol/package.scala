package shipreq.webapp.base

import boopickle.Pickler
import scalaz.\/
import shipreq.base.util.ErrorMsg
import shipreq.webapp.base.event.VerifiedEvent

package object protocol {
  import BinCodecEvents._

  implicit class MemberExt_ServerSideProcProtocol(private val self: ServerSideProc.Protocol.type) extends AnyVal {
    def toEvents[I: Pickler](name: String): ServerSideProc.Protocol[I, ErrorMsg \/ VerifiedEvent.Seq] =
      ServerSideProc.Protocol(name)
  }

}
