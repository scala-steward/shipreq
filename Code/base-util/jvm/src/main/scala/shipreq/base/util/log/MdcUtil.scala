package shipreq.base.util.log

import org.slf4j.MDC
import shipreq.base.util.FxModule._

object MdcUtil {

  def preserve[A](f: Fx[A]): Fx[Fx[A]] =
    Fx {
      val mdcState = MDC.getCopyOfContextMap
      if (mdcState eq null)
        f
      else
        Fx {
          MDC.setContextMap(mdcState)
          try
            f.unsafeRun()
          finally
            MDC.clear()
        }
    }

  def preserveUnsafe[A](f: () => A): () => A = {
    val g = preserve[A](Fx(f())).unsafeRun()
    () => g.unsafeRun()
  }
}
