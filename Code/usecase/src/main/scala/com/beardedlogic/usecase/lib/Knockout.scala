package com.beardedlogic.usecase.lib

import net.liftweb.http.js.{JsCmds, JsCmd, JsExp}

/**
 * Javascript commands and expressions for working with Knockout.js.
 *
 * @since 12/06/2013
 */
object KnockoutJs {
  def ApplyBindings(modelExpr: String): JsCmd = JsCmds.Run(s"ko.applyBindings($modelExpr)")
  def ApplyBindings(modelName: String, json: String): JsCmd = JsCmds.Run(s"ko.applyBindings(new $modelName($json))")
}
