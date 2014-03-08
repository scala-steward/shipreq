package com.beardedlogic.shipreq.db

import scala.slick.session.Session
import org.slf4j.LoggerFactory

/**
 * Template/mixin for database singletons.
 */
trait DbTemplate {

  protected val log = LoggerFactory.getLogger(getClass)

  // ===================================================================================================================
  // Connection

  protected def establishConnection(): BaseDbConnection

  protected val baseConn = establishConnection()
  import baseConn.{DataSource, Slick}

  @inline final def DatabaseName = baseConn.DatabaseName

  // ===================================================================================================================
  // Initialisation

  private[this] val migrator = new DbMigrator(DataSource)
  private[this] def initLock: AnyRef = migrator

  @volatile private[this] var initPending = true

  def init(): Unit = initLock.synchronized {
    if (initPending) {
      migrator.performPendingMigrations()
      Slick.withTransaction((s: Session) => onInit(s))
      initPending = false
      log.debug("Database initialised successfully.")
    }
  }

  protected def onInit(implicit s: Session): Unit

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
