package shipreq.webapp.base

import WebappConfig.assetPath_/

object URLs {
  lazy val SvgSortAsc           = assetPath_/ + "sort-asc.svg"
  lazy val SvgSortBlank         = assetPath_/ + "sort-blank.svg"
  lazy val SvgSpinner           = assetPath_/ + "loading-spin.svg"
  lazy val SvgShipreqCircleDark = assetPath_/ + "shipreq-circle-dark.svg"

  lazy val PageLogout = "/logout"
}
