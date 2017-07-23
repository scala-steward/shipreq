package shipreq.webapp.base.util

import Preload._

/**
  * see https://w3c.github.io/preload/
  */
final case class Preload(href       : String,
                         as         : As,
                         rel        : Rel = Rel.Preload,
                         `type`     : String = "",
                         crossorigin: Boolean = false) {
  def absoluteHref: Boolean = href.contains("://")
  def relativeHref: Boolean = !absoluteHref
}

object Preload {

  sealed abstract class Rel(final val value: String)
  object Rel {
    /** Optional and low-priority fetch for a resource that might be used by a subsequent navigation */
    case object Prefetch extends Rel("prefetch")

    /** Mandatory and high-priority fetch for a resource that is necessary for the current navigation */
    case object Preload extends Rel("preload")
  }

  sealed abstract class As(final val value: String)
  object As {
//    case object Audio extends As("audio")
//    case object Video extends As("video")
//    case object Track extends As("track")
    case object Script extends As("script")
    case object Style extends As("style")
    case object Font extends As("font")
//    case object Image extends As("image")
//    case object Fetch extends As("fetch")
    case object Worker extends As("worker")
//    case object Embed extends As("embed")
//    case object Object extends As("object")
//    case object Document extends As("document")
  }

  def style(href: String) = Preload(href, As.Style)
  def script(href: String) = Preload(href, As.Script)
  def font(href: String, `type`: String) = Preload(href, As.Font, `type` = `type`, crossorigin = true)
  def fontWoff2(href: String) = font(href, "font/woff2")
}