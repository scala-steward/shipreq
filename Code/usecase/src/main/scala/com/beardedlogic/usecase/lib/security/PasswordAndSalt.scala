package com.beardedlogic.usecase.lib.security

import org.apache.shiro.util.ByteSource
import org.apache.shiro.codec.Base64
import org.apache.shiro.crypto.hash.SimpleHash
import Oshiro._

/**
 * A hashed password and the salt used to generate the hash.
 */
case class PasswordAndSalt(hashedPassword: String, saltBytes: ByteSource) {
  def salt = saltBytes.toBase64
}

/**
 * Methods for creating and generating passwords & salt.
 */
object PasswordAndSalt {
  def apply(hashedPassword: String, salt: String): PasswordAndSalt = new PasswordAndSalt(hashedPassword, ByteSource.Util.bytes(Base64.decode(salt)))

  def apply(hashedPassword: SimpleHash, salt: ByteSource): PasswordAndSalt = new PasswordAndSalt(hashedPassword.toBase64, salt)

  def hash(plainTextPassword: String, salt: ByteSource): PasswordAndSalt = {
    val hash = new SimpleHash(HashingAlgorithm, plainTextPassword, salt, HashingIterations)
    apply(hash, salt)
  }

  def hashWithRandomSalt(plainTextPassword: String) = hash(plainTextPassword, RNG.nextBytes())
}