package shipreq.webapp.base.lib

import japgolly.scalajs.react.Key

/**
 * Generate React keys.
 */
final class ReactKeyGen {
  private var i = 0

  def next(): Key = {
    i += 1
    i
  }
}

object ReactKeyGen {
  val global = new ReactKeyGen
}
