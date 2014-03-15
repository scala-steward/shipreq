package shipreq.taskman

import java.util.Properties
import shipreq.base.util._
import shipreq.base.db._


object ExampleUsage {
  val runMode = RunMode.detect()
  val props = JPropertiesValueReader(Props.standard(runMode)(new Properties))
  import props._
  val db = new DbTemplate {
    override protected lazy val connection = DatabaseConnection.establish_!()
  }
  db.init()
}

////    import scala.slick.session.Session
////    import scala.slick.jdbc.{StaticQuery => Q}
////    import Q.interpolation
////
////    Slick.withSession { implicit s: Session =>
////      val count= sql"SELECT count(1) FROM yay".as[Int].first
////      println(s"-------- COUNT = $count")
