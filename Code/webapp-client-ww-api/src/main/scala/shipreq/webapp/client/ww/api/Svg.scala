package shipreq.webapp.client.ww.api

import boopickle.DefaultBasic._

final case class Svg(content: String) extends AnyVal

object Svg {
  implicit val pickler: Pickler[Svg] =
    transformPickler(Svg.apply)(_.content)
}