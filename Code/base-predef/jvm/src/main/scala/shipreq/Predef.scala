package shipreq

// JVM
object Predef extends PredefShared {

  override implicit def predefExtString(s: String): AnyVal with PredefShared.ExtString =
    new PredefJvm.ExtString(s)
}

object PredefJvm {
  import java.lang.String

  final class ExtString(private val s: String) extends AnyVal with PredefShared.ExtString {
    override def quote =
      io.circe.Encoder.encodeString(s).noSpaces
  }

}
