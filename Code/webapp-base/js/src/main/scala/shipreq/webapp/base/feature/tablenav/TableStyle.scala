package shipreq.webapp.base.feature.tablenav

import shipreq.webapp.base.feature.TableNavigationFeature.Bundle

final case class TableStyle(hasRowSpans: Boolean)

object TableStyle {
  implicit def toTableStyle(implicit b: Bundle): TableStyle = b.tableStyle
}