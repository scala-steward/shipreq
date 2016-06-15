package shipreq.webapp.gen.transform

import scala.xml.Unparsed
import shipreq.webapp.gen._

abstract class Transformer[Data](val templates: MainAndTest[Html], val data: MainAndTest[Data]) {
  def apply(d: Data): Html

  def xml(d: Data): Unparsed =
    Unparsed(apply(d).value)
}

object Transformer {

  val All: List[Transformer[_]] =
    List(ProjectSpaLoader)

}