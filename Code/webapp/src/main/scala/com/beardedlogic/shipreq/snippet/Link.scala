package com.beardedlogic.shipreq.snippet

import com.beardedlogic.shipreq.app.AppSiteMap.Implicits._
import com.beardedlogic.shipreq.lib.SnippetHelpers
import net.liftweb.http.DispatchSnippet
import net.liftweb.sitemap.{Loc, SiteMap}
import scala.collection.concurrent.TrieMap
import scala.util.hashing.Hashing
import scala.xml.{NodeSeq, Text}
import scalaz.Memo

/**
 * Creates a link to a page. Throws an error is the page is not found.
 */
object Link extends DispatchSnippet with SnippetHelpers {

  override def dispatch = { case name => linkTo(name) }

  def linkTo(name: String): NodeSeq => NodeSeq = {
    val loc = SiteMap.findLoc(name) openOrThrowException s"Unable to generate link to $name"
    linkMemo(loc)
  }

  private val linkMemo =
    Memo.mutableMapMemo(new TrieMap[Loc[_], NodeSeq => NodeSeq](Hashing.default, Equiv.reference))(generateLink)

  def generateLink(loc: Loc[_]): NodeSeq => NodeSeq = {
      val linkText = loc.linkText openOr Text(loc.name)
      val link = <a href={loc.relativeUrl}>{linkText}</a>
      _ => link
    }
}
