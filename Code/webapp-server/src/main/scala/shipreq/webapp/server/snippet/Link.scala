package shipreq.webapp.server.snippet

import shipreq.webapp.server.app.AppSiteMap
import shipreq.webapp.server.app.AppSiteMap.Implicits._
import shipreq.webapp.server.lib.{Misc, SnippetHelpers}
import shipreq.webapp.base.AppConsts
import net.liftweb.http.DispatchSnippet
import net.liftweb.sitemap.{Loc, SiteMap}
import scala.xml.{Elem, Node, NodeSeq, Null, Text, UnprefixedAttribute}

/**
 * Creates a link to a page. Throws an error is the page is not found.
 */
object Link extends DispatchSnippet with SnippetHelpers {

  private type R = NodeSeq => NodeSeq

  override def dispatch = {
    case "App" => appLink
    case name  => toPage(name)
  }

  val appLink =
    staticHtml(<a href={AppSiteMap.Home.absoluteUrl}>{AppConsts.appName}</a>)

  private def generatePageLink(loc: Loc[_]): R = {
    val linkText = loc.linkText openOr Text(loc.name)
    val a = new UnprefixedAttribute("href", loc.relativeUrl, Null)
    n => n match {
      case Elem(prefix, label, attrs, ns, ch@_*) =>
        val inner: Seq[Node] = if (ch.nonEmpty) ch else linkText.theSeq
        val newAttr = attrs.remove("data-lift").append(a)
        Elem(prefix, label, newAttr, ns, false, inner: _*)
    }
  }

  private val pageLinkMemo =
    Misc.newMemo[Loc[_], R](Equiv.reference)(generatePageLink)

  def toPage(name: String) = {
    val loc = SiteMap.findLoc(name) openOrThrowException s"No page found in sitemap called '$name'"
    pageLinkMemo(loc)
  }
}
