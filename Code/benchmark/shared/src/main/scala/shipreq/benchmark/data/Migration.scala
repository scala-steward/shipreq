package shipreq.benchmark.data

object Migration {

  implicit def idsAreNowIntsInsteadOfLongs(l: Long): Int = l.toInt

}
