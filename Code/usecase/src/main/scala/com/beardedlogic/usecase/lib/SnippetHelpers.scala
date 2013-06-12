package com.beardedlogic.usecase.lib

import net.liftweb.http.js.{JsCmd, JsExp}
import com.beardedlogic.usecase.lib.msg.{JavaScriptReaction, Reactor}
import com.beardedlogic.usecase.model.DAO

/**
 * @since 11/06/2013
 */
trait SnippetHelpers {

  @inline implicit final def jsExpToJsCmd(in: JsExp) = in.cmd

  type JsCallback = () => JsCmd

  def jsCallback(f: Reactor => Any): JsCallback = () => JavaScriptReaction(f(_))

  def jsCallbackWithDao(f: (Reactor, DAO) => Any): JsCallback =
    jsCallback(r =>
      DAO.withTransaction(dao =>
        f(r, dao)
      ))

}
