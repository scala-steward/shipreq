package shipreq.webapp.base

import shipreq.base.util.IndexLabel
import shipreq.webapp.base.util.EnvMacros
import IndexLabel._

object AppConsts {

  val appName = "ShipReq"

  /** The URL path under which AJAX requests are serviced. */
  val ajaxPath = "A"

  val assetPath   = EnvMacros.devOrRel("/dev", "/a")
  val assetPath_/ = assetPath + "/"

  /** Passwords' min & max lengths. */
  val passwordLength = 8 to 128

  /** Usernames' min & max lengths. */
  val usernameLength = 3 to 32

  /** Email address max length. */
  final val emailMaxLength = 120

  /** Limit for generic VARCHAR columns. */
  final val shortTextMaxLength = 255

  /** Limit the length of seemingly-unbound inputs. Prevents a malicious user creating 1GB rows. */
  final val largeTextMaxLength = 20000


  // (UC-8.)0.1.a.i.4
  val UseCaseStepLabels = Vector[IndexLabel](
    NumericFrom0,
    NumericFrom1,
    Alpha,
    Roman,
    NumericFrom1)

  /**
   * Maximum number of levels (inclusive) where the root (no steps) is 0.
   */
  val useCaseStepsMaxDepth = UseCaseStepLabels.length

  /**
   * Maximum number of children per parent (inclusive).
   */
  final val useCaseStepsMaxLength = 99
}
