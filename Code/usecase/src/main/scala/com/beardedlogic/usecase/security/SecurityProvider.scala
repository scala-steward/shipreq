package com.beardedlogic.usecase
package security

import db.UserDescriptor

/**
 * Interface that provides the app with security features.
 */
trait SecurityProvider {
  def loggedInUser: Option[UserDescriptor]
}