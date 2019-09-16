package shipreq.utils

import japgolly.microlibs.stdlib_ext.StdlibExt._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import shipreq.base.util.FxModule._

object UtilUtils {

  def writeFile(filename: String, content: String): Unit =
    Files.write(Paths get filename, content getBytes StandardCharsets.UTF_8)

  def logTime[A](msg: String)(f: => A): A = {
    System.out.println(msg)
    System.out.flush()
    val (a, dur) = Fx(f).measureDuration.unsafeRun()
    System.out.println(s"    (${dur.conciseDesc})")
    System.out.flush()
    a
  }

}
