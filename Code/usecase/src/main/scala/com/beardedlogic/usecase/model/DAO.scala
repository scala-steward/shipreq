package com.beardedlogic.usecase.model

import scala.slick.driver.PostgresDriver.simple._
import java.sql.Connection
import com.beardedlogic.usecase.lib.db.DB
import com.beardedlogic.usecase.lib._

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
          with FieldKeyAccessor
          with FieldValueAccessor
          with FieldListAccessor
          with StepAccessor
          with UseCaseAccessor
          with RelationAccessor {

  override implicit val db = _session

  def conn = db.conn
  def withTransaction[T](f: => T): T = db.withTransaction(f)
  def close() = db.close
  def rollback() = db.rollback
}

object DAO {

  def withInstance[T](transaction: Boolean)(block: DAO => T): T = {
    if (transaction) withTransaction(block) else withSession(block)
  }

  def withSession[T](block: DAO => T): T = DB.Slick.withSession(initConnAndExec(_, block))
  def withTransaction[T](block: DAO => T): T = DB.Slick.withTransaction(initConnAndExec(_, block))

  @inline private def initConnAndExec[T](s: Session, block: DAO => T): T = {
    s.conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)
    block(new DAO(s))
  }

  def get = new DAO(DB.Slick.createSession())

  def forSession[M[_]] = new ResourceLeaseMonad1[DAO, M] {protected override def exec[T](f: DAO => T): T = DAO.withSession(f(_))}
  def forSessionLeft[R, M[_, R]] = new ResourceLeaseMonadL[DAO, R, M] {protected override def exec[T](f: DAO => T): T = DAO.withSession(f(_))}
  def forSessionRight[L, M[L, _]] = new ResourceLeaseMonadR[DAO, L, M] {protected override def exec[T](f: DAO => T): T = DAO.withSession(f(_))}

  def forTransaction[M[_]] = new ResourceLeaseMonad1[DAO, M] {protected override def exec[T](f: DAO => T): T = DAO.withTransaction(f(_))}
  def forTransactionLeft[R, M[_, R]] = new ResourceLeaseMonadL[DAO, R, M] {protected override def exec[T](f: DAO => T): T = DAO.withTransaction(f(_))}
  def forTransactionRight[L, M[L, _]] = new ResourceLeaseMonadR[DAO, L, M] {protected override def exec[T](f: DAO => T): T = DAO.withTransaction(f(_))}
}