package shipreq.base.util

sealed trait LeftRight extends IsoBool[LeftRight] {
  override final def companion = LeftRight
}

object LeftRight extends IsoBool.Object[LeftRight] {
  override def positive = Right
  override def negative = Left

  case object Right extends LeftRight
  case object Left  extends LeftRight
}
