package shipreq.base.util.log

import com.typesafe.scalalogging.{Logger => ScalaLogger}
import java.util.concurrent.ConcurrentHashMap

object HasLogger {
  private[this] val cache =
    new ConcurrentHashMap[Class[_], ScalaLogger]()

  private[this] val create: java.util.function.Function[Class[_], ScalaLogger] =
    c => {
      // object A { object B } = A$B$
      var n = c.getTypeName
      if (n.endsWith("$"))
        n = n.dropRight(1)
      n = n.replace('$', '.')
      ScalaLogger(n)
    }

  private[HasLogger] def forClass(c: Class[_]): ScalaLogger =
    cache.computeIfAbsent(c, create)

  abstract class ForClass(loggerCls: Class[_]) {
    final protected implicit val logger: ScalaLogger =
      HasLogger.forClass(loggerCls)
  }
}

trait HasLogger {
  final protected implicit val logger: ScalaLogger =
    HasLogger.forClass(getClass)
}
