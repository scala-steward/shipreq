package shipreq.base.util

import japgolly.microlibs.stdlib_ext.MutableArray
import japgolly.microlibs.stdlib_ext.StdlibExt._
import japgolly.microlibs.utils.AsciiTable
import java.time.Duration

object Debug {

  trait Implicits {
    final implicit def debugAnyExt[A](a: A): DebugAnyExt[A] =
      new DebugAnyExt(a)
  }

  object Implicits extends Implicits

  final class DebugAnyExt[A](private val self: A) extends AnyVal {
    def tapPrint()            : A = {println(self); self}
    def tapPrint(title: Any)  : A = {println(s"$title: $self"); self}
    def tapPrintF(f: A => Any): A = {println(f(self)); self}
  }

  // ===================================================================================================================

  def logDuration[A](run: => A): A =
    logDuration(None, run)

  def logDuration[A](name: String, run: => A): A =
    logDuration(Some(name), run)

  def logDuration[A](name: Option[String], run: => A): A = {
    val start = System.nanoTime()
    try
      run
    finally {
      val end = System.nanoTime()
      val dur = Duration.ofNanos(end - start)
      name match {
        case Some(n) => println(s"$n completed in ${dur.conciseDesc}")
        case None    => println(s"Completed in ${dur.conciseDesc}")
      }
    }
  }

  // ===================================================================================================================

  private final class MutInt(init: Int) {
    var value = init
  }

  def CallCounter(): CallCounter =
    new CallCounter

  final class CallCounter {
    private[this] val lock = new AnyRef
    private var stats = Map.empty[String, MutInt]

    def clear(): Unit =
      lock.synchronized {
        stats = Map.empty[String, MutInt]
      }

    def inc(name: String): Unit = {
      val n = name.trim
      lock.synchronized {
        stats.get(n) match {
          case Some(m) => m.value += 1
          case None    => stats = stats.updated(n, new MutInt(1))
        }
      }
    }

    def apply[A](name: String)(a: => A): A = {
      inc(name)
      a
    }

    def report(): String =
      lock.synchronized {
        val rows = MutableArray(stats).sortBy(_._1).iterator().map { case (n, m) => Seq(n, "%,10d".format(m.value))}.toList
        val content = Seq("NAME", "COUNT") :: rows
        AsciiTable(content)
      }

    def printReport(): Unit =
      println(report())
  }
}