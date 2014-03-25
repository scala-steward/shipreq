package shipreq.base.test.db.specs2

import org.specs2.mutable.Specification
import org.specs2.execute.{Result, AsResult}
import org.specs2.specification.AroundExample
import java.util.Properties
import scala.slick.session.Session
import scala.slick.jdbc.SQLInterpolation
import shipreq.base.util._
import shipreq.base.db.{DatabaseConnection, DbTemplate}

object TestDB {
  val runMode = RunMode.Test
  val props = JPropertiesValueReader(Props.loadUsingStandardStrategy(runMode)(new Properties))
  import props._
  private object db extends DbTemplate {
    override protected lazy val connection = DatabaseConnection.establish_!()
    def slick = _slick
  }

  def slick = db.slick

  @volatile private var ready = false

  def init(): Unit = synchronized {
    if (!ready) {
      ready = true
      db.wipe_!()
      db.init()
    }
  }

}

trait DatabaseTest extends AroundExample {
  this: Specification =>

  private val dbLog = Logger.forClass(getClass)

  isolated

  private[this] var _session: Option[Session] = None
  implicit def session: Session = _session.getOrElse(throw new RuntimeException("No session available."))

  override def around[T: AsResult](t: => T): Result = {
    TestDB.init() // TODO needs some kind of shutdown hook, keeps crashing from SBT with ~test
    TestDB.slick.withTransaction(s => {
      _session = Some(s)
      try AsResult(t)
      finally {
        try s.rollback() catch {case e: Throwable => dbLog.warn("Rollback failed.", e)}
        _session = None
      }
    })
  }

  implicit def sqlInterpolation(s: StringContext) = new SQLInterpolation(s)
}
