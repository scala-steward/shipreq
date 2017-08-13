package shipreq.webapp.server

import japgolly.microlibs.config._
import japgolly.microlibs.config.ConfigParser.Implicits.Defaults._
import japgolly.microlibs.config.JavaTimeConfigParsers._
import java.time.Duration
import monocle.macros.Lenses
import scalaz.syntax.applicative._
import shipreq.base.ops.StackdriverTrace
import shipreq.base.util._

@Lenses
final case class ServerConfig(

    baseUrl: Url.Absolute.Base,

    /** A short amount of time, unnoticeable to humans, to sleep in order to frustrate automated security attacks. */
    attackFrustrationDelay: Duration,

    /** Number of characters in tokens used for email & reset-password verification. */
    securityTokenLength: Int,

    /** How long registration tokens are valid for after issuing. */
    registrationTokenLifespan: Duration,

    /** How long password-reset tokens are valid for after issuing. */
    passwordResetTokenLifespan: Duration,

    /**
    * Whether or not public registrations are allowed.
    * (Registration tokens already issued will still be accepted.)
    */
    publicRegistration: Permission,

    /** The DB schema in which the Taskman interfaces reside. */
    taskmanSchema: String,

    initTaskmanOnBoot: Boolean,
    initTaskmanRetry: RetryCriteria,

    trace: Option[StackdriverTrace.Cfg]) {

  val attackFrustrationDelayMs: Long =
    attackFrustrationDelay.toMillis
}

object ServerConfig {

  def config: Config[ServerConfig] =
    ( Config.need    [String  ]      ("url").map(Url.Absolute.Base.apply) |@|
      Config.need    [Duration]      ("attack_frustration_delay") |@|
      Config.need    [Int     ]      ("token.length") |@|
      Config.need    [Duration]      ("token.lifespan.register") |@|
      Config.need    [Duration]      ("token.lifespan.resetpw") |@|
      Config.getOrUse[Boolean ]      ("feature.publicRegistration", true).map(Allow.when) |@|
      Config.need    [String  ]      ("taskman.schema") |@|
      Config.getOrUse[Boolean ]      ("taskman.init", true) |@|
      RetryCriteria.config.withPrefix("taskman.init.retry.") |@|
      StackdriverTrace.config.withPrefix("trace.")
    ) (apply).withPrefix("shipreq.")

}