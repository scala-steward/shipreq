package shipreq.webapp.server.security

import japgolly.univeq._
import org.apache.shiro.util.ByteSource
import org.apache.shiro.codec.Base64
import org.apache.shiro.crypto.hash.SimpleHash
import Oshiro._

/** A hashed string. */
final case class HashedStr(value: String) extends AnyVal
object HashedStr {
  implicit def univEq: UnivEq[HashedStr] = UnivEq.derive
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

final case class Salt(byteSource: ByteSource) extends AnyVal {
  def toBase64: String =
    byteSource.toBase64

  def hash(plainTextPassword: String): HashedStr =
    HashedStr(new SimpleHash(HashingAlgorithm, plainTextPassword, byteSource, HashingIterations).toBase64)
}
object Salt {
  def random(): Salt =
    apply(RNG.nextBytes())

  def fromBase64(base64: String): Salt =
    apply(ByteSource.Util.bytes(Base64.decode(base64)))
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * A hashed password and the salt used to generate the hash.
 */
final case class PasswordAndSalt(hashedPassword: HashedStr, salt: Salt) {
  def matches(plainTextPassword: String): Boolean =
    salt.hash(plainTextPassword) ==* hashedPassword
}

/**
 * Methods for creating and generating passwords & salt.
 */
object PasswordAndSalt {

  def create(plainTextPassword: String, salt: Salt): PasswordAndSalt =
    PasswordAndSalt(salt.hash(plainTextPassword), salt)

  def createWithRandomSalt(plainTextPassword: String): PasswordAndSalt =
    create(plainTextPassword, Salt.random())
}