package shipreq.webapp.app

import net.liftweb.util.Helpers._
import org.joda.time.Period
import shipreq.base.util.ExternalValueReader._
import shipreq.base.util.jodatime.JodaTimeValueRetrievers
import shipreq.webapp.util.PropsRetrievers._
import shipreq.webapp.util.ExpireAfter

object AppConfig {
  implicit def PropScope = scopeByNS("shipreq")
  private val jtr = JodaTimeValueRetrievers(retrieverS)
  import jtr.retrieverPeriod
  private implicit def rts: Retriever[TimeSpan] = jtr.retrieverPeriod.map(p => p)

  val SupportEmailAddress = need[String]("support.email")

  val BaseUrl = need[String]("url")

  /** A short amount of time, unnoticeable to humans, to sleep in order to frustrate automated security attacks. */
  val AttackFrustrationDelayMs = need[Period]("attack_frustration_delay").toStandardDuration.getMillis

  /** Number of characters in tokens used for email & reset-password verification. */
  val ConfirmationTokenLength = need[Int]("token.length")

  /** The DB schema in which the Taskman interfaces reside. */
  val TaskmanSchema = need[String]("taskman.schema")

  /** How long confirmation tokens are valid for after issuing. */
  val TokenLifespan = need[TimeSpan]("token.lifespan.email_conf")

  /** How long password-reset tokens are valid for after issuing. */
  val PasswordResetTokenLifespan = need[TimeSpan]("token.lifespan.resetpw")

  /** The amount of time that a user is allowed to view a share after authenticating, without re-authenticating. */
  val ShareViewAuthPeriod = need[Period]("share.auth_period")

  /** Maximum time a flash variable will be retained. (default) */
  val FlashVarTTL = Period seconds 12

  val QuoteCachePolicy = ExpireAfter(Period minutes 30)

  val DemoUseCaseMaxSteps = 50

  /**
   * Whether or not new registrations are allowed.
   * (Registration tokens already issued will still be accepted.)
   */
  var AllowRegister: () => Boolean = { // non-volatile var allowed because modification will only occur in test-mode.
    val v = tryNeed("allow.register", true)
    () => v
  }

  val jQueryVersion = "2.1.1"

  /** URL prefix for dev & test only assets */
  val devAssetPath = "/assets/dev"

  /** URL prefix for vendor assets */
  val vendorAssetPath = "/assets/vendor"
}
