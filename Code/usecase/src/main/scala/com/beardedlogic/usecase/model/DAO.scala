package com.beardedlogic.usecase.model

import scala.slick.driver.PostgresDriver.simple._
import com.beardedlogic.usecase.lib.db.DB

/**
 * Provides database connectivity.
 *
 * Methods should follow this pattern:
 * - `create`: Creates a new row. May or may not have an existing `data` or `value` row.
 * - `createInitial`: Creates a value for the first time. A new `data` row is created, and the value revision is 1.
 * - `find`: Searches for a single row. Returns Option[T].
 * - `findAll`: Searches for a multiple rows. Returns List[T].
 * - `findOrCreate`: Searches for an item and creates it if not found.
 * - `sync`: Syncs the database to a given object. If DB is up-to-date, nothing happens, else a new value is created.
 */
trait DatabaseAccessor {
  implicit def db: Session
}

/**
 * Single, monolithic interface to the database.
 */
class DAO(_session: Session)
  extends DataAccessor
          with ValueAccessor
          with RelationAccessor
          with FieldKeyAccessor
          with FieldValueAccessor
          with FieldListAccessor
          with StepAccessor {
  override implicit val db = _session

  def withTransaction[T](f: => T): T = db.withTransaction(f)
  def close() = db.close
  def rollback() = db.rollback
}

object DAO {

  def withSession[T](block: DAO => T): T = {
    DB.Slick.withSession { db => block(new DAO(db)) }
  }

  def withTransaction[T](block: DAO => T): T = {
    DB.Slick.withTransaction { db => block(new DAO(db)) }
  }

  def get = new DAO(DB.Slick.createSession())
}
