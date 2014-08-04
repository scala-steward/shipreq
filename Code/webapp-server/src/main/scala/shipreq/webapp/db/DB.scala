package shipreq.webapp.db

import scala.slick.jdbc.JdbcBackend.Session
import shipreq.base.db.{DatabaseConnection, DbTemplate}

/**
 * Database connectivity.
 *
 * @since 21/05/2013
 */
object DB extends DbTemplate {

  override protected def newConnection = {
    import shipreq.webapp.util.PropsRetrievers._
    DatabaseConnection.establish_!()
  }

  @inline def DataSource = connection.ds

  // Making public for tests
  override def wipe_!(): Unit = super.wipe_!()

  override protected def onInit(implicit s: Session) = {
    FieldKeyType.init
  }

  object DaoProvider extends DaoProvider {
    private[this] val slick = _slick // avoid lazy val overhead
    override def withRawSession[T](f: Session => T): T = slick.withSession(f)
    override protected def rawSession(): Session       = slick.createSession()
  }
}
