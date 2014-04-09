package shipreq.taskman.server

import java.util.Properties
import org.joda.time.{DateTime, Period}
import scala.slick.session.Database
import shipreq.base.db.{DatabaseConnection, DbTemplate}
import shipreq.base.util.ExternalValueReader._
import shipreq.base.util._
import shipreq.base.util.jodatime.JodaTimeHelpers._
import shipreq.base.util.jodatime.JodaTimeValueRetrievers
import shipreq.taskman.api.CfgKeys
import shipreq.taskman.api.Types._
import shipreq.taskman.server.business.{BusinessLogic, Failure, Email}
import scalaz.effect.IO

//==========================================================================================

class Db(props: StringBasedValueReader) extends DbTemplate {
  import props._

  override protected def newConnection = DatabaseConnection.establish_!()

  def slick = _slick
}

//==========================================================================================

class TaskmanCtx(db: Database, mailProps: Properties, evr: StringBasedValueReader)
  extends Email.Ctx[EmailImpl.EA] with EmailImpl.Ctx with BopImpl.Ctx with Logger {
  import EmailImpl.EA

  implicit val sopReifier = new SopImpl(db)

  protected def fromDb = CfgValueReader(sopReifier)
  protected implicit def scope: PropScope = scopeByNS("taskman")
  protected implicit def retrieverS = evr.retrieverS
  val jtr = JodaTimeValueRetrievers(retrieverS)

  import evr.retrieverI
  import jtr.retrieverPeriod

  override val mailSession = EmailImpl.loadSession(mailProps)
  override val addrParser  = EmailImpl.AddressParser
  override val emailer     = new EmailImpl(this)
  private[this] implicit def rEA = retrieverS.map(s => addrParser(s.tag[IsEmailAddr]))

  override val defaultFromAddress = validate("mail.from", need[EA])(valTestNotError)
  override val shipreq  = need(CfgKeys.Webapp.appName )(GlobalScope, fromDb.retrieverS)
  override val loginUrl = need(CfgKeys.Webapp.loginUrl)(GlobalScope, fromDb.retrieverS)

  object server {
    private implicit def scope: PropScope = scopeByNS("taskman.server")
    def atLeast(min: Period) =
      valTest[Period](_.toStandardDuration isLongerThan min.toStandardDuration, s"Must be at least $min.")
    def atLeast(min: Int) =
      valTest[Int](_ >= min, s"Must be at least $min.")

    val queueSize = validate("queueSize", need[Int])(atLeast(1))
    val trustPeriod = validate("trustPeriod", need[Period])(atLeast(10 seconds))
    val pollEvery = validate("poll.every", need[Period])(atLeast(50 ms))
    val pollGap = validate("poll.min", n => getO[Period](n) getOrElse pollEvery)(atLeast(50 ms))
    if (pollGap.toStandardDuration isLongerThan pollEvery.toStandardDuration)
      log.warn(s"The minimum poll gap ($pollGap) is larger than the poll time ($pollEvery). Wasteful.")
  }

  def loggable = Map[String, Any](
    "defaultFromAddress" -> defaultFromAddress
    , "shipreq" -> shipreq
    , "loginUrl" -> loginUrl
    , "server.queueSize" -> server.queueSize
    , "server.trustPeriod" -> server.trustPeriod
    , "server.poll.every" -> server.pollEvery
    , "server.poll.gap" -> server.pollGap
  )
  loggable.toList
    .sortBy(kv => (kv._1.count(_ == '.'), kv._1))
    .map{case (k,v) => "Config: %-20s = %s".format(k,v) }
    .foreach(log.info)

  implicit val bopReifier = new BopImpl(this)
  implicit val failurePolicy = Failure.failurePolicy
  implicit val msgProcessor = BusinessLogic(this, bopReifier)
  implicit val clock = IO(new DateTime)

  implicit val nodeId = sopReifier.getNextNodeId.unsafePerformIO()
  log.info("Node ID is {}.", nodeId.value)
}
