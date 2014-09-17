package shipreq.webapp.snippet

import shipreq.webapp.app.AppConfig
import shipreq.webapp.app.AppSiteMap
import shipreq.webapp.app.AppSiteMap.Implicits._
import shipreq.webapp.lib.{Misc, SnippetHelpers}
import net.liftweb.http.DispatchSnippet
import net.liftweb.sitemap.{Loc, SiteMap}
import net.liftweb.util.Props
import net.liftweb.util.Props.RunModes.{Test, Development}
import scala.xml.{Elem, Node, NodeSeq, Null, Text, UnprefixedAttribute}

/**
 * Creates a link to a page. Throws an error is the page is not found.
 */
object Link extends DispatchSnippet with SnippetHelpers {

  private type R = NodeSeq => NodeSeq

  override def dispatch = {
    case "App"      => appLink
    case "jquery"   => jqueryLink
    case "clientJs" => clientJs
    case name       => ToPage(name)
  }

  private def static(link: NodeSeq): R = _ => link

  val appLink =
    static(<a href={AppSiteMap.Home.absoluteUrl}>{AppConfig.AppName}</a>)

  val jqueryLink = {
    val jqueryUrl = Props.mode match {
      case Development | Test => s"${AppConfig.devAssetPath}/jquery.js"
      case _                  => s"//ajax.googleapis.com/ajax/libs/jquery/${AppConfig.jQueryVersion}/jquery.min.js"
    }
    static(<script type="text/javascript" src={jqueryUrl}></script>)
  }

  val clientJs = {
    val reactUrl = Props.mode match {
      case Development | Test => s"${AppConfig.devAssetPath}/react.js"
      case _                  => s"${AppConfig.vendorAssetPath}/react.js"
    }
    val clientJsUrl = Props.mode match {
      case Development | Test => s"${AppConfig.devAssetPath}/webapp-client-fastopt.js"
      case _                  => "/assets/C.js"
    }
    static(
      <script type="text/javascript" src={reactUrl}></script>
      <script type="text/javascript" src={clientJsUrl}></script>
    )
  }

  object ToPage {
    def apply(name: String): R = {
      val loc = SiteMap.findLoc(name) openOrThrowException s"No page found in sitemap called '$name'"
      pageLinkMemo(loc)
    }

    private val pageLinkMemo =
      Misc.newMemo[Loc[_], R](Equiv.reference)(generatePageLink)

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
  }
}
