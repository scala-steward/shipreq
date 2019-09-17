package shipreq.utils

import japgolly.microlibs.stdlib_ext.StdlibExt._
import shipreq.base.util.FxModule._

object UtilUtils {

  def logTime[A](msg: String)(f: => A): A = {
    System.out.println(msg)
    System.out.flush()
    val (a, dur) = Fx(f).measureDuration.unsafeRun()
    System.out.println(s"    (${dur.conciseDesc})")
    System.out.flush()
    a
  }

}
