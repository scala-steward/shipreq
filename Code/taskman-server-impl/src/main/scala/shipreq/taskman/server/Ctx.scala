package shipreq.taskman.server

import java.util.Properties
import org.joda.time.{DateTime, Period}
import scala.slick.session.Database
import scalaz.effect.IO
import shipreq.base.db.{DatabaseConnection, DbTemplate}
import shipreq.base.util.ExternalValueReader._
import shipreq.base.util._
import shipreq.base.util.jodatime.JodaTimeHelpers._
import shipreq.base.util.jodatime.JodaTimeValueRetrievers
import shipreq.base.util.log.HasLogger
import shipreq.taskman.api.CfgKeys
import shipreq.taskman.api.Types._
import shipreq.taskman.server.business.{BusinessLogic, Failure, Email}

//==========================================================================================

class Db(props: StringBasedValueReader) extends DbTemplate {
  import props._

  override protected def newConnection = DatabaseConnection.establish_!()

  def slick = _slick
}

//==========================================================================================

class TaskmanCtx(db: Database, mailProps: Properties, evr: StringBasedValueReader)
  extends Email.Ctx[EmailImpl.EA] with EmailImpl.Ctx with BopImpl.Ctx with HasLogger {
  import EmailImpl.EA

  protected def fromDb = SopImpl.cfgValueReader(db)
  protected implicit def scope: PropScope = scopeByNS("taskman")
  protected implicit def retrieverS = evr.retrieverS
  val jtr = JodaTimeValueRetrievers(retrieverS)

  import evr.retrieverI
  import jtr.retrieverPeriod

  override val mailSession = EmailImpl.loadSession(mailProps)
  override val addrParser  = EmailImpl.AddressParser
  override val emailer     = new EmailImpl(this)
  private[this] implicit def rEA = retrieverS.map(s => addrParser(s.tag[IsEmailAddr]))
  private[this] implicit def rEE = EmailImpl.envelopeLoader

  override val publicFrom = validate("mail.public.from", need[EA])(valTestNotError)
  override val supportEnv = need[Email.Envelope[EA]]("mail.support")
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
      log.warn.z(s"The minimum poll gap ($pollGap) is larger than the poll time ($pollEvery). Wasteful.")
  }

  def propmap = List[(String, Any)](
      "shipreq"            -> shipreq
    , "loginUrl"           -> loginUrl
    , "mail.public.from"   -> publicFrom
    , "mail.support"       -> supportEnv
    , "server.queueSize"   -> server.queueSize
    , "server.trustPeriod" -> server.trustPeriod
    , "server.poll.every"  -> server.pollEvery
    , "server.poll.gap"    -> server.pollGap
  )
  def logContent(): Unit = {
    for ((k,v) <- propmap)
      log.info.fmt("Config: %-20s = %s", k, v)
    log.info.z(s"Node ID is ${nodeId.value}.")
  }

  implicit val bopReifier = new BopImpl(this)
  implicit val sopReifier = new SopImpl(db, this, bopReifier)
  implicit val failurePolicy = Failure.failurePolicy
  implicit val msgProcessor = BusinessLogic(this, bopReifier)
  implicit val clock = IO(new DateTime)
  implicit val nodeId = sopReifier.getNextNodeId.unsafePerformIO()
}
