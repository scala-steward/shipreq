package shipreq.webapp.member.protocol.indexeddb

import scala.scalajs.js
import scala.scalajs.js.|

final case class IndexedDbKey(value: Int | String) {
  def asJs = value.asInstanceOf[js.Any]
}
