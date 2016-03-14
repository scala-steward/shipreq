package shipreq.webapp.server
package app

import net.liftweb.common.Logger
import scalaz.{Name, Need}
import db.DaoT

object Defaults extends Logger {

  private def dbVal[V](fn: DaoT => V): Name[V] = Need(DI.DaoProvider.vend.withTransaction(fn))

  def uninit(): Unit = {
  }
  uninit()

  def init(): Unit = {
    // debug("Defaults initialised successfully.")
  }
}