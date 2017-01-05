package shipreq.base.util

import japgolly.microlibs.config._
import scalaz.std.list.listInstance
import scalaz.effect.IO

object Props {

  def fileSources: Sources[IO] =
    Source.propFileOnClasspath[IO]("shipreq.props", optional = false)

  def sources: Sources[IO] =
    Source.environment[IO] >
    fileSources >
    Source.system[IO]

}
