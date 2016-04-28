package shipreq.webapp.server.security

import shipreq.webapp.server.data.UserDescriptor

/**
 * Interface that provides the app with security features.
 */
trait SecurityProvider {

  def loggedInUser: Option[UserDescriptor]

  /**
   * Sleep for a short amount of time, unnoticeable to humans, in order to frustrate automated security attacks.
   */
  def enforceHumanSpeed(): Unit
}