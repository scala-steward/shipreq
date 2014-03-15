package shipreq.base.db

import com.googlecode.flyway.core.Flyway
import org.slf4j.LoggerFactory
import scala.slick.session.{Database, Session}

/**
 * Template/mixin for database singletons.
 */
trait DbTemplate {

  protected val log = LoggerFactory.getLogger(getClass.getCanonicalName.replaceFirst("\\$$", ""))

  protected /* lazy */ val connection: DatabaseConnection

  @inline final def DatabaseName = connection.name

  protected val slick = Database.forDataSource(connection.ds)

  protected def flywayCfg: Flyway => Flyway = identity

  private[this] val migrator = new DbMigrator(connection, flywayCfg)

  // ===================================================================================================================
  // Initialisation

  private[this] def initLock: AnyRef = migrator
  @volatile private[this] var initPending = true

  def init(): Unit = initLock.synchronized {
    if (initPending) {
      migrator.performPendingMigrations()
      slick.withTransaction((s: Session) => onInit(s))
      initPending = false
      log.debug("Database initialised successfully.")
    }
  }

  protected def onInit(implicit s: Session): Unit = ()

  /**
   * Drops all objects (tables, views, procedures, triggers, ...) in the configured schemas.
   */
  def wipe_!() = initLock.synchronized {
    log.warn("Wiping database: " + DatabaseName)
    migrator.wipe_!()
    initPending = true
    this
  }
}
