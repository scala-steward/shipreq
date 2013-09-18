package com.beardedlogic.usecase.lib

import java.lang.{Long => JLong}
import net.liftweb.common.{Full, Empty, Box}
import com.beardedlogic.usecase.util.BaseX
import Types._

object ExternalId {

  // util.Random.shuffle(x.toList).mkString
  final val UseCase = new ExternalId[UseCaseIdentIdTag]("0atxlQwnj7y3zFZNVBqJ42AcriYEeMu8SdU91HgfTsb6GhmWkX5KopCIRLvOPD")
  final val TextRev = new ExternalId[TextRevIdTag]("eBM0xKQuO2Zy43AnWGPmkbXN9HprwV7ItSi1CdETv6D5UYRscjJzhFgoLflqa8")
}

final class ExternalId[Tag <: TypeTag[Long]](val dictionaryStr: String) {
  type Id = JLong @@ Tag

  val base62 = new BaseX(dictionaryStr, 4)
  require(base62.base.longValue == 62)

  private final val ExternalIdRegex = "^[a-zA-Z0-9]{4,11}$".r.pattern

  @inline private final def splitLong(x: Long): (Int, Int) = ((x >>> 32).toInt, (x.toInt & 0xffffffff))
  @inline private final def joinInts(a: Int, b: Int): Long = (a.toLong << 32L) | (b & 0xffffffffL)
  @inline private final def xorness(b: Int) = b ^ ((b & 0x7e7) << 12)

  @inline final def apply(internal: Id): String = toExternal(internal)

  def toExternal(internal: Id): String = {
    var (a, b) = splitLong(internal.longValue)
    b = xorness(b)
    b = shuffleBitsObfuscate(b)
    val x = joinInts(a, b)
    base62.encode(x)
  }

  private def parse(external: String): Id = {
    val y = base62.decode(external)
    var (a, b) = splitLong(y)
    b = shuffleBitsRestore(b)
    b = xorness(b)
    joinInts(a, b).tag[Tag]
  }

  def isValidExternalId(str: String): Boolean = ExternalIdRegex.matcher(str).matches

  def parseO(external: String): Option[Id] = if (isValidExternalId(external)) Some(parse(external)) else None

  def parseB(external: String): Box[Id] = if (isValidExternalId(external)) Full(parse(external)) else Empty
  def parseB(str: Box[String]): Box[Id] = str.flatMap(parseB)

  // -------------------------------------------------------------------------------------------------------------------
  // http://stackoverflow.com/questions/8554286/obfuscating-an-id

  private final val mask1 = 0x00550055
  private final val mask2 = 0x0000cccc
  private final val bits1 = 7
  private final val bits2 = 14

  @inline private final def shuffleBitsObfuscate(x: Int): Int = {
    var t = (x ^ (x >> bits1)) & mask1
    val u = x ^ t ^ (t << bits1)
    t = (u ^ (u >> bits2)) & mask2
    u ^ t ^ (t << bits2)
  }
  @inline private final def shuffleBitsRestore(y: Int): Int = {
    var t = (y ^ (y >> bits2)) & mask2
    val u = y ^ t ^ (t << bits2)
    t = (u ^ (u >> bits1)) & mask1
    u ^ t ^ (t << bits1)
  }
}
