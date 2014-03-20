package shipreq.taskman.server

import shipreq.base.util._
import java.util.Properties
import shipreq.base.db.{DatabaseConnection, DbTemplate}

class Db(props: StringBasedValueReader) extends DbTemplate {
  import props._

  override protected lazy val connection = DatabaseConnection.establish_!()

  def slick = _slick
}

object Main {

  val log = Logger.forClass(getClass)

  def main(args: Array[String]) {

    import shipreq.base.util.ExternalValueReader._

    implicit def scope = GlobalScope

    // Determine run mode
    implicit val _rm = RunMode.retrieverFromSysProps
    val runMode: RunMode.Value = tryNeed("run.mode", RunMode.detectFromStackTrace())
    log.info("Run mode: {}", runMode)

    // Config
    val props = Props.loadUsingStandardStrategy(runMode)(new Properties)
    val propsR = JPropertiesValueReader(props)
    import propsR._

    // Init database
    val db = new Db(propsR)
    db.init()

    // Mail
    import javax.mail._
    import javax.mail.internet._
    val mailAuth: Option[Authenticator] = {
      implicit def scope = scopeByNS("mail")

      getO[String]("user").map(user => {
        log.info("Mail user: {}", user)
        new Authenticator {
          override def getPasswordAuthentication =
            new PasswordAuthentication(user, need[String]("password"))}
      })
    }
    val session = Session.getInstance(props, mailAuth getOrElse null)

    // Send an email
    val m = new MimeMessage(session)
    m.setFrom("hello@whatever.com")
    m.setRecipients(Message.RecipientType.TO, "japgolly@gmail.com")
    m.setSubject("TEST -- HELLO!")
    m.setSentDate(new java.util.Date)
    m.setText("This is a test email. Great.")
    log.info("Sending...")
    Transport.send(m)
    log.info("DONE!")
  }
}
