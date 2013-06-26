package com.beardedlogic.usecase.model

import UserAccessor._
import scala.slick.jdbc.{StaticQuery => Q}

/**
 * @since 26/06/2013
 */
object UserAccessor {

  val GetPasswordAndSaltByUsername = Q.query[String, (String, String)]("SELECT password, password_salt FROM usr WHERE username=?")
  val GetPasswordAndSaltByEmail = Q.query[String, (String, String)]("SELECT password, password_salt FROM usr WHERE email=? AND password IS NOT NULL")
}

trait UserAccessor extends DatabaseAccessor {

  def findUserPasswordAndSalt(usernameOrEmail: String) =
    if (usernameOrEmail.indexOf('@') == -1)
      findUserPasswordAndSaltByUsername(usernameOrEmail)
    else
      findUserPasswordAndSaltByEmail(usernameOrEmail)

  def findUserPasswordAndSaltByUsername(username: String): Option[(String, String)] = GetPasswordAndSaltByUsername.firstOption(username)

  def findUserPasswordAndSaltByEmail(email: String): Option[(String, String)] = GetPasswordAndSaltByEmail.firstOption(email)
}