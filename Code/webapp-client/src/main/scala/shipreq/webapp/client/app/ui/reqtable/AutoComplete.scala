package shipreq.webapp.client.app.ui.reqtable

import japgolly.scalajs.jquery.{TextComplete => TC}
import shipreq.base.util.Util
import shipreq.webapp.base.data._
import shipreq.webapp.base.text.Grammar
import TC.{Strategies, Strategy}

object AutoComplete {

  def hashtag(legal: Stream[HashRefKey], prefix: Boolean): Strategy = {
    import Grammar.{hashRefKey => G}
    val searchFn    = TC.caseInsensitiveContains(legal.map(_.value).sorted)
    val prefixRegex = Util.regexEscapeAndWrap(G.prefix)
    val mainRegex   = s"(${G.firstChar.one}${G.allChars.*})$$"

    if (prefix)
      Strategy(s"$prefixRegex$mainRegex", index = 1)
        .search(searchFn)
        .replace(G.prefix + _ + " ")
    else
      Strategy(s"(^|\\s)$prefixRegex?$mainRegex", index = 2)
        .search(searchFn)
        .replace("$1" + _ + " ")
  }

  def hashtag(legalIssues: Stream[CustomIssueType], legalTags: Stream[ApplicableTag], prefix: Boolean): Strategy =
    hashtag(legalIssues.map(_.key) append legalTags.map(_.key), prefix)

  def tag(legal: Stream[ApplicableTag], prefix: Boolean): Strategy =
    hashtag(legal.map(_.key), prefix)

  def issue(legal: Stream[CustomIssueType], prefix: Boolean): Strategy =
    hashtag(legal.map(_.key), prefix)

}
