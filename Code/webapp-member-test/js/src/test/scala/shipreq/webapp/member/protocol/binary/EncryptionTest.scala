package shipreq.webapp.member.protocol.binary

import japgolly.scalajs.react.AsyncCallback
import shipreq.base.test.BaseTestUtil._
import shipreq.base.test.Node
import shipreq.base.util.BinaryData
import utest._

object EncryptionTest extends TestSuite {

  private def newEncryption(key: BinaryData): AsyncCallback[Encryption] =
    Encryption(Node.webCrypto, key).map(_.getOrThrow("webCrypto not available"))

  private implicit def binaryDataFromString(str: String): BinaryData = {
    val bytes = str.getBytes
    assert(bytes.length == str.length)
    BinaryData.unsafeFromArray(bytes)
  }

  override def tests = Tests {

    "main" - Node.asyncTest {

      val key1 = "x" * 32
      val key2 = "y" * 32
      val src1 = "hello there!"
      val src2 = "awesome"

      for {
        e1    <- newEncryption(key1)
        enc1  <- e1.encrypt(src1)
        enc1b <- e1.encrypt(src1)
        dec1  <- e1.decrypt(enc1)
        dec1b <- e1.decrypt(enc1b)

        e2   <- newEncryption(key2)
        enc2 <- e2.encrypt(src2)
        e2b  <- newEncryption(key2)
        dec2 <- e2b.decrypt(enc2)

        bad12 <- e1.decrypt(enc2).attempt
        bad21 <- e2.decrypt(enc1).attempt

      } yield {
        val srcBin1: BinaryData = src1
        val srcBin2: BinaryData = src2

        // round trip
        assertEq(dec1, srcBin1)
        assertEq(dec1b, srcBin1)
        assertEq(dec2, srcBin2)

        // non-determinism
        assertNotEq(enc1, enc1b)

        // key protection
        assert(bad12.isLeft)
        assert(bad21.isLeft)

        s"""
           |src1  = ${srcBin1.describe()}
           |enc1  = ${enc1.describe()}
           |enc1b = ${enc1b.describe()}
           |src2  = ${srcBin2.describe()}
           |enc2  = ${enc2.describe()}
           |bad   = $bad12
           |""".stripMargin.trim
      }
    }

  }
}
