package shipreq.webapp.db

import scala.slick.session.Session
import shipreq.base.db.{DatabaseConnection, DbTemplate}

/**
 * Database connectivity.
 *
 * @since 21/05/2013
 */
object DB extends DbTemplate {

  override protected lazy val connection = {
    import shipreq.webapp.util.PropsRetrievers._
    DatabaseConnection.establish_!()
  }

  @inline def DataSource = connection.ds

  override protected def onInit(implicit s: Session) = {
    FieldKeyType.init
  }

  object DaoProvider extends DaoProvider {
    override def withRawSession[T](f: Session => T): T = _slick.withSession(f)
    override protected def rawSession(): Session       = _slick.createSession()
  }
}
