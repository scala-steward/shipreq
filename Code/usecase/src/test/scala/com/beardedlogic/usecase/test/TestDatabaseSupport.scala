package com.beardedlogic.usecase
package test

import com.googlecode.flyway.core.dbsupport.{SqlScript, DbSupportFactory}
import java.sql.Connection
import net.liftweb.common.Logger
import org.apache.commons.io.IOUtils
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{Exceptional, Outcome, Suite}
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.session.Session
import scala.util.Random
import Q.interpolation

import lib.db.{DaoProvider, DB}
import lib.DI
import model.DAO

object TestDatabaseSupport {

  @volatile private var ready = false

  def init() {
    synchronized {
      if (!ready) {
        ready = true
        DB.wipe_!
        (new bootstrap.liftweb.Boot).boot
      }
    }
  }

  val Random = new Random()

}

trait TestDatabaseSupport extends TestHelpers with ShouldMatchers with Logger {
  self: Suite =>

  override protected def withFixture(test: NoArgTest): Outcome = {
    TestDatabaseSupport.init()
    debug(s"DB Test start: ${test.name}")
    try {
      val outcome = withTransactionInternal(wrapTestsInTransaction, wrapTestsInTransaction) {
        beforeEachWithDao()
        test()
      }
      outcome match {
        case Exceptional(e) => debug("Test failure.", e)
        case _ =>
      }
      outcome
    }
    catch {case e: Throwable => error("Test error.", e); throw e }
    finally debug(s"DB Test end: ${test.name}")
  }

  private def withTransactionInternal[U](transaction: Boolean, rollback: Boolean)(fn: => U): U =
    DB.withInstance(transaction) { s: Session =>
      val oldSessionVar = this.sessionVar
      val oldDbVar = this.dbVar
      try {
        this.sessionVar = s
        this.dbVar = new DAO(s)
        s.conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)
        DI.DaoProvider.doWith(testDaoProvider) {
          fn
        }
      }
      finally {
        if (rollback) s.rollback()
        this.sessionVar = oldSessionVar
        this.dbVar = oldDbVar
      }
    }

  def beforeEachWithDao() {}

  val wrapTestsInTransaction = true

  var sessionVar: Session = null
  implicit def session = {
    if (sessionVar == null) throw new IllegalStateException("Trying to access DB session outside of test.")
    sessionVar
  }

  var dbVar: DAO = null
  def db = dbVar

  def withNewTransaction[U](fn: => U): U = withTransactionInternal(true, true)(fn)

  def rollbackAfter[U](fn: => U): U = db.withTransaction {
    val result = fn
    db.rollback()
    result
  }

  def testDaoProvider = new TestDaoProvider(db)

  def randomId = -TestDatabaseSupport.Random.nextLong().abs

def countRowsIn(table: Symbol): Int = ???
def assertTableDiffs[T](expectations: (Symbol, Int)*)(block: => T): T = ???
def truncate(tables: Symbol*): Unit = ???

  def countRowsIn2(table: Table) = Q.queryNA[Int](s"select count(*) from ${table.name}").first

  sealed trait Table {def name: String; override def toString = name}
  object tFieldKeyType extends Table {def name = "field_key_type"}
  object tFieldKey extends Table {def name = "field_key"}
  object tUsecase extends Table {def name = "usecase"}
  object tUsecaseRev extends Table {def name = "usecase_rev"}
  object tText extends Table {def name = "text"}
  object tTextRev extends Table {def name = "text_rev"}
  object tUcField extends Table {def name = "uc_field"}
  object tUsr extends Table {def name = "usr"}
  val Tables = List(tFieldKeyType, tFieldKey, tUsecase, tUsecaseRev, tText, tTextRev, tUcField, tUsr)

  def assertTableDiffs2[T](expectations: (Table, Int)*)(block: => T) = {
    val specTables = expectations.map(_._1)
    val unspecTables = Tables.filter(!specTables.contains(_)).map((_,0))
    val fullExp = expectations ++ unspecTables
    val fullExpMap = fullExp.toMap

    def count = fullExp.map { case (t, _) => (t -> countRowsIn2(t)) }.toMap
    val before = count
    val result = block
    val after = count.map { case (t, newCount) => (t, newCount - before(t)) }.toMap

    if (after != fullExp) {
      val badKeys = after.keys.filter(k=> after(k) != fullExpMap(k)).toSet
      val a = after.filter(e => badKeys.contains(e._1))
      val e= fullExpMap.filter(e => badKeys.contains(e._1))
      a should be(e)
    }

    result
  }

  def truncateAll() = truncate2(Tables: _*)

  def truncate2(tables: Table*) {
    tables.foreach { table =>
    // Dependents first
      table match {
        case tFieldKeyType => truncate2(tFieldKey)
        case tFieldKey     => truncate2(tText)
        case tUsecase      => truncate2(tUsecaseRev)
        case tUsecaseRev   => Q.updateNA(s"update usecase set latest_rev_id = NULL").execute; truncate2(tUcField)
        case tText         => truncate2(tTextRev)
        case tTextRev      => truncate2(tUcField)
        case _             =>
      }
      val tableName = table.name
      Q.updateNA(s"delete from $tableName").execute
    }
  }

  def lookupConfirmationToken(email: String) = sql"select confirmation_token from usr where email = $email".as[String].firstOption

  /**
   * Loads an SQL script on the classpath, and runs it.
   */
  def runSqlScript(filename: String) {
    val dbSupport = DbSupportFactory.createDbSupport(session.conn)
    val sqlFull = IOUtils.toString(getClass.getResource(filename.replaceFirst("^/?", "/")))
    val script = new SqlScript(sqlFull, dbSupport)
    script.execute(dbSupport.getJdbcTemplate)
  }
}

class TestDaoProvider(dao: DAO) extends DaoProvider {
  override def get = dao
  override def withSession[T](block: DAO => T): T = block(dao)
  override def withTransaction[T](block: DAO => T): T = block(dao)
}