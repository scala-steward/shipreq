package shipreq.base.util

import japgolly.microlibs.config._
import scalaz.std.list.listInstance
import shipreq.base.util.FxModule._

object Props {

  def fileSources: Sources[Fx] =
    Source.propFileOnClasspath[Fx]("shipreq.properties", optional = true) >
    Source.propFileOnClasspath[Fx]("secret.properties", optional = true)

  def sources: Sources[Fx] =
    Source.environment[Fx] >
    fileSources >
    Source.system[Fx]
}