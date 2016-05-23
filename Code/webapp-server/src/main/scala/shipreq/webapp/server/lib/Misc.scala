package shipreq.webapp.server.lib

import net.liftweb.http.S
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeUtils, Period}
import scala.annotation.tailrec
import scala.util.Random
import shipreq.base.util.log.HasLogger
import shipreq.webapp.server.ServerConfig

object Misc extends Misc {

  val RNG = new Random()

//  val Iso8601Format = ISODateTimeFormat.dateTime.withZoneUTC
  val Iso8601Format = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC

  final class DateTimeExt(private val t: DateTime) extends AnyVal {
    def >(timeToLive: Period): Boolean =
      isExpired_?(t, timeToLive)

    def <=(timeToLive: Period): Boolean =
      ! >(timeToLive)

    def toStringIso8601: String =
      Iso8601Format.print(t)
  }
}

trait Misc extends HasLogger {
  import Misc._

  implicit def DateTimeExt(v: DateTime): DateTimeExt =
    new DateTimeExt(v)

  def clientIp(): Option[String] = (
    S.originalRequest.filter(_.request ne null).map(_.remoteAddr)
      or S.containerRequest.map(_.remoteAddress)
      or S.request.filter(_.request ne null).map(_.remoteAddr)
    // println("X-Real-IP: " + req.header("X-Real-IP"))
    // println("X-Forwarded-For: " + req.header("X-Forwarded-For"))
    )

  def isExpired_?(startTime: DateTime, timeToLive: Period, now: Long = DateTimeUtils.currentTimeMillis): Boolean =
    startTime plus timeToLive isBefore now

  def randomString(length: Int): String =
    RNG.alphanumeric.take(length).mkString

  def randomConfirmationToken(): String =
    randomString(ServerConfig.ConfirmationTokenLength)

  @tailrec
  final def retry[T](n: Int, firstError: Option[Throwable] = None)(fn: => T): T = {
    import scala.util.{Failure, Success, Try}
    Try { fn } match {
      case Success(result)      => result
      case Failure(e) if n > 0  => retry(n - 1, firstError orElse Some(e))(fn)
      case Failure(e) if n <= 0 =>
        firstError.foreach(log.debug("First retry failure.", _))
        throw e
    }
  }
}