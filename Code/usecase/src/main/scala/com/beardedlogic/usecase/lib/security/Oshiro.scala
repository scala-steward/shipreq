package com.beardedlogic.usecase.lib.security

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.credential.HashedCredentialsMatcher
import org.apache.shiro.crypto.SecureRandomNumberGenerator
import org.apache.shiro.crypto.hash.{SimpleHash, Sha512Hash}
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.util.ByteSource

/**
 * Apache城との橋になる「お城」。
 */
object Oshiro {

  val HashingAlgorithm = Sha512Hash.ALGORITHM_NAME
  val HashingIterations = 1007

  val RNG = new SecureRandomNumberGenerator()

  def init() {
    val CredentialsManager = new HashedCredentialsMatcher(HashingAlgorithm)
    CredentialsManager.setHashIterations(HashingIterations)
    CredentialsManager.setStoredCredentialsHexEncoded(false)

    val Realm = new AppSecurityRealm
    Realm.setCredentialsMatcher(CredentialsManager)

    val SecurityManager = new DefaultSecurityManager(Realm)
    SecurityUtils.setSecurityManager(SecurityManager)
  }

  def hash(password: String, salt: ByteSource) = new SimpleHash(HashingAlgorithm, password, salt, HashingIterations)

  /**
   * Creates a new, random salt value and uses it to hash a plaintext password.
   *
   * @param password Plaintext password.
   * @return Two base-64 encoded strings: password and salt. These strings can be stored in the database directly.
   */
  def hashWithRandomSalt(password: String): (String, String) = {
    val salt = RNG.nextBytes()
    (hash(password, salt).toBase64, salt.toBase64)
  }
}