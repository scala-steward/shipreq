package com.beardedlogic.usecase.util

import net.liftweb.http.js.{JsCmds, JsCmd}

/** Type definition of a JavaScript reaction. */
case object JavaScript extends ReactionType[JsCmd]

/** Builds a JavaScript reaction. */
class JavaScriptReaction extends ReactionBuilder[JsCmd] with Reactor {
  var _result: JsCmd = JsCmds.Noop
  override def reactor = this
  override def result = _result
  override def apply[R](t: ReactionType[R])(f: => R): Unit = JavaScript.unpack(t, f)(js => this << js)
  def <<(js: JsCmd) { _result &= js }
}

object JavaScriptReaction {
  /** Convenience method for creating and returning JavaScript reaction. */
  def apply(fn: JavaScriptReaction => Unit): JsCmd = {
    val rb = new JavaScriptReaction
    fn(rb.reactor)
    rb.result
  }
}