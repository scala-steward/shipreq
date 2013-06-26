package com.beardedlogic.usecase
package lib.security

import org.apache.shiro.realm.AuthenticatingRealm
import org.apache.shiro.authc._
import org.apache.shiro.util.ByteSource
import org.apache.shiro.codec.Base64
import model.DAO

/**
 * Bridge between Shiro and this app. Performs authentication checks.
 *
 * @since 25/06/2013
 */
class AppSecurityRealm extends AuthenticatingRealm {

  override protected def doGetAuthenticationInfo(token: AuthenticationToken) = {
    // Parse input
    val userPassToken =  token.asInstanceOf[UsernamePasswordToken]
    val usernameOrEmail = userPassToken.getUsername

    // Query database
    val r = DAO.withSession(_.findUserPasswordAndSalt(usernameOrEmail))
    if (r.isEmpty) throw new UnknownAccountException("No account found for [" + usernameOrEmail + "]")
    val (password,salt) = r.get

    // Result
    val info = new SimpleAuthenticationInfo("unused right now, will probably be either user id or model later", password, getName)
    info.setCredentialsSalt(ByteSource.Util.bytes(Base64.decode(salt)))
    info
  }
}
