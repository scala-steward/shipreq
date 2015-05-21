package shipreq.webapp.client.util

import scalaz.\/
import shipreq.base.util.IsoBool

sealed trait Validity

object Validity extends IsoBool.ObjOnly[Validity] {
  override protected def pos = Valid
  override protected def neg = Invalid

  def apply(d: Any \/ Any): Validity =
    Valid <~ d.isRight
}

case object Valid extends Validity with IsoBool[Validity] {
  override protected def neg = Invalid
}

case object Invalid extends Validity with IsoBool[Validity] {
  override protected def neg = Valid
}
