package shipreq.webapp.feature

import scalaz.Validation
import shipreq.webapp.lib.Types._

package object validation {

  type ValidationResultU[+R]          = Validation[VFailure, R]
  type ValidationResult[+R <: AnyRef] = Validation[VFailure, R @@ Validated]

}
