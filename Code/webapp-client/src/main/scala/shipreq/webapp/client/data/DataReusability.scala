package shipreq.webapp.client.data

import japgolly.scalajs.react.extra.Reusability
import shipreq.webapp.base.data._

object DataReusability {

  implicit val reusabilityProject: Reusability[Project] = Reusability.byRef

}
