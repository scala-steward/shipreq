package com.beardedlogic.usecase.lib

import net.liftweb.util.CssSel
import net.liftweb.util.Helpers.strToCssBindPromoter

object HtmlTransformExt {

  final val PassThru = "dpp_recommends_this_oh_well" #> ""

  def IfCssSel(cond: => Boolean)(expr: => CssSel): CssSel = if (cond) expr else PassThru

}
