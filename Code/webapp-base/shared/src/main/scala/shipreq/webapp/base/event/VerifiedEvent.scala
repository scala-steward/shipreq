package shipreq.webapp.base.event

import shipreq.webapp.base.hash.{ProjectHash, HashScheme}

/**
 * A verified event is an event that has been validated by the server, proven applicable, and retains a hash expected
 * of the Project after application.
 *
 * @param hash The hash of the Project after event application.
 */
case class VerifiedEvent(hashScheme: HashScheme,
                         hash      : Int,
                         event     : Event) {

  def projectHash = ProjectHash(hashScheme, hash)
}