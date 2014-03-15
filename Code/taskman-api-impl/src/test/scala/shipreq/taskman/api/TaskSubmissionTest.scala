package shipreq.taskman.api

import java.util.Properties
import org.specs2.mutable._
import shipreq.taskman.{Props, RunMode}
import shipreq.base.util.JPropertiesValueReader
import shipreq.base.db.{DatabaseConnection, DbTemplate}

object TestDB {

  val runMode = RunMode.Test
  val props = JPropertiesValueReader(Props.standard(runMode)(new Properties))
  import props._

  private object db extends DbTemplate {
    override protected lazy val connection = DatabaseConnection.establish_!()
  }

  @volatile private var ready = false

  def init(): Unit = synchronized {
    if (!ready) {
      ready = true
      db.wipe_!()
      db.init()
    }
  }
}

class TaskSubmissionTest extends Specification {

  "blah" in {
    TestDB.init()
    pending
  }

}
