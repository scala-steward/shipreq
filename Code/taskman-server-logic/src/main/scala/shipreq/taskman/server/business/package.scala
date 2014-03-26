package shipreq.taskman.server

import scalaz.~>

package object business {

  type BopReifier = Bop ~> IOE

}
