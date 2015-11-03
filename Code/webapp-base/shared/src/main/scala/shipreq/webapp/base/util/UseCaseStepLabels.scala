package shipreq.webapp.base.util

import scala.annotation.tailrec
import shipreq.base.util.{ParseInt, RomanNumeral}

object UseCaseStepLabels {

  sealed trait LevelLabeler {

    /**
     * @param index [0,max)
     */
    def label(index: Int): String

    /**
     * @return [0,max)
     */
    def parse(label: String): Option[Int]

    def labelTmp(index: Int)    = label(index - 1)
    def parseTmp(label: String) = parse(label).map(_ + 1) getOrElse sys.error(s"Can't parse [$label]")
  }

  object Numeric0 extends LevelLabeler {
    override def label(index: Int)    = index.toString
    override def parse(label: String) = ParseInt.unapply(label)
    override def labelTmp(index: Int)    = label(index)
    override def parseTmp(label: String) = parse(label) getOrElse sys.error(s"Can't parse [$label]")
  }

  object Numeric1 extends LevelLabeler {
    override def label(index: Int)    = (index + 1).toString
    override def parse(label: String) = ParseInt.unapply(label).map(_ - 1)
  }

  object Roman1 extends LevelLabeler {
    override def label(index: Int)    = RomanNumeral(index + 1).toLowerCase
    override def parse(label: String) = RomanNumeral.parse(label).map(_ - 1)
  }

  object Alpha1 extends LevelLabeler {
    private final val First = 'a'

    override def label(index: Int) = {
      assert(index >= 0, s"Alpha1.label($index)")
      @tailrec
      def go(n: Int, s: String): String = {
        val q = n / 26
        val r = n % 26
        val cur = (r + First).toChar.toString
        val s2 = if (s eq null) cur else cur + s
        if (q == 0)
          s2
        else
          go(q - 1, s2)
      }
      go(index, null)
    }

    override def parse(label: String) = {
      var ok = true
      var sum = 0
      for (c <- label) {
        val v = c - First + 1
        if (v <= 0 || v > 26)
          ok = false
        else
          sum = sum * 26 + v
      }
      if (ok)
        Some(sum - 1)
      else
        None
    }
  }

  // (1.)0.1.a.i.4
  final val Labelers = Vector(Numeric0, Numeric1, Alpha1, Roman1, Numeric1)
}