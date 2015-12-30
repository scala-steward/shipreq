package shipreq.webapp.server.lib

import scala.xml.{Text, NodeSeq}
import scalaz.old.NonEmptyList
import shipreq.webapp.server.util.ListFlashVar

object NoticeFlash {

  sealed class NoticeFlashVar extends ListFlashVar[NodeSeq] {
    def addS(msg: String) = add1(Text(msg))
    def addS(msgs: NonEmptyList[String]) = add(msgs.map(Text.apply))
  }

  val errors = new NoticeFlashVar
  val warnings = new NoticeFlashVar
  val notices = new NoticeFlashVar
}
