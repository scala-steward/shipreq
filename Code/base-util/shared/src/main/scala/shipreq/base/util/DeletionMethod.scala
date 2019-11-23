package shipreq.base.util

sealed trait DeletionMethod extends IsoBool[DeletionMethod] {
  override final def companion = DeletionMethod
}

object DeletionMethod extends IsoBool.Object[DeletionMethod] {
  override def positive = Soft
  override def negative = Hard

  case object Soft extends DeletionMethod
  case object Hard extends DeletionMethod
}
