package com.beardedlogic.usecase.lib.security

import org.apache.shiro.SecurityUtils
import org.apache.shiro.config.IniSecurityManagerFactory
import org.apache.shiro.crypto.SecureRandomNumberGenerator
import org.apache.shiro.crypto.hash.SimpleHash
import org.apache.shiro.util.ByteSource

/**
 * Apache城との橋になる「お城」。
 */
object Oshiro {

  private val factory = new IniSecurityManagerFactory("classpath:shiro.ini")
  private val ini = factory.getIni

  final val HashingAlgorithm = ini.getSection("main").get("cm.hashAlgorithmName")
  final val HashingIterations = ini.getSection("main").get("cm.hashIterations").toInt

  final val RNG = new SecureRandomNumberGenerator()

  def init() {
    val securityManager = factory.getInstance
    SecurityUtils.setSecurityManager(securityManager)
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