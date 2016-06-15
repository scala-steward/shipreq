package shipreq.webapp.client.base

import japgolly.scalajs.react.vdom.prefix_<^._
import scala.scalajs.js

package object ui {

  /** React no longer allows style to be Strings.
    *
    * This is both a helper method to ease style creation, and a means of easily keeping track of all places where
    * we create style objects.
    */
  @inline def inlineStyle(f: js.Dynamic.type => js.Object with js.Dynamic): TagMod =
    ^.style := (f(js.Dynamic): js.Object)

}
