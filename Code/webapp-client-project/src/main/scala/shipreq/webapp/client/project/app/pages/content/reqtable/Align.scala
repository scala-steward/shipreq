package shipreq.webapp.client.project.app.pages.content.reqtable

import shipreq.base.util._

sealed trait Align extends IsoBool[Align] {
  override final def companion = Align
}

object Align extends IsoBool.Object[Align] {
  override def positive = Left
  override def negative = Right
}

case object Left extends Align
case object Right extends Align
