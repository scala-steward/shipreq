package shipreq.webapp.base.protocol

import boopickle.Pickler
import japgolly.univeq.UnivEq
import BoopickleMacros._
import BinCodecGeneric._

final case class ErrorMsg(msg: String)
object ErrorMsg {
  implicit def univEq: UnivEq[ErrorMsg] = UnivEq.derive
  implicit val pickleErrorMsg: Pickler[ErrorMsg] = pickleCaseClass
}
