package com.beardedlogic.shipreq
package db

import scala.slick.session.Session

/**
 * Database connectivity.
 *
 * @since 21/05/2013
 */
object DB extends DbTemplate {

  override protected def establishConnection() = {
    import util.PropsRetrievers._
    new BaseDbConnection()
  }

  override protected def onInit(implicit s: Session) = {
    FieldKeyType.init
  }

  @inline def DataSource = baseConn.DataSource
  import baseConn.Slick

  object DaoProvider extends DaoProvider {
    override def withRawSession[T](f: Session => T): T = Slick.withSession(f)
    override protected def rawSession(): Session       = Slick.createSession()
  }
}
