package com.beardedlogic.usecase.lib.msg

import net.liftweb.http.js.{JsCmds, JsCmd}

/** Type definition of a JavaScript reaction. */
case object JavaScript extends ReactionType[JsCmd]

/** Builds a JavaScript reaction. */
class JavaScriptReactionBuilder extends ReactionBuilder[JsCmd] with Reactor {
  override def reactor = this
  override var result: JsCmd = JsCmds.Noop
  override def apply[R](t: ReactionType[R])(f: => R): Unit = JavaScript.unpack(t, f)(r => result &= r)
}
