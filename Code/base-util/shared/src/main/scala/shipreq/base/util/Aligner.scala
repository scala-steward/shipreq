package shipreq.base.util

object Aligner {

  def forStrings(): Mutable[String] =
    new Mutable(_.length)

  final class Mutable[I](f: I => Int) {
    private[this] var maxLen = 0

    def consider(i: I): Unit = {
      val len = f(i)
      if (len > maxLen)
        maxLen = len
    }

    def paddingSize(len: Int): Int =
      if (len >= maxLen) 0 else maxLen - len
  }
}
