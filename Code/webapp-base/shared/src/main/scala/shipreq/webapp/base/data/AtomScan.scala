package shipreq.webapp.base.data

import japgolly.nyaya.util.Multimap
import shipreq.base.util.UnivEq
import shipreq.webapp.base.text.{Atom, Text}
import Atom._

/**
 * Scanning includes dead reqs.
 *
 * @param codeRefs ReqCodes referenced in anything anywhere (including text in dead custom-text fields).
 */
class AtomScan(val tagsInLiveReqText: Multimap[ReqId,     Set,    ApplicableTagId],
               val tagsInAllReqText : Multimap[ReqId,     Set,    ApplicableTagId],
               val issuesInReqText  : Multimap[ReqId,     Vector, AnyIssue],
               val issuesInGroupText: Multimap[ReqCodeId, Vector, AnyIssue],
               val codeRefs         : Set[ReqCodeId])

object AtomScan {
  def apply(p: Project): AtomScan = {
    var tagsInLiveReqText: Multimap[ReqId,     Set,    ApplicableTagId] = UnivEq.emptySetMultimap
    var tagsInAllReqText : Multimap[ReqId,     Set,    ApplicableTagId] = UnivEq.emptySetMultimap
    var issuesR          : Multimap[ReqId,     Vector, AnyIssue]        = UnivEq.emptyMultimap
    var issuesG          : Multimap[ReqCodeId, Vector, AnyIssue]        = UnivEq.emptyMultimap
    var codeRefs         : Set[ReqCodeId]                               = UnivEq.emptySet

    def scan(liveText : Live,
             reqId    : ReqId     = null,
             reqCodeId: ReqCodeId = null)
            (text: Text.AnyOptional): Unit = {

      def go(as: Text.AnyOptional): Unit =
        as foreach {
          case _: Literal         # Literal
             | _: ReqRef          # ReqRef
             | _: PlainTextMarkup # EmailAddress
             | _: PlainTextMarkup # WebAddress
             | _: PlainTextMarkup # MathTeX
             | _: NewLine         # BlankLine => ()

          case a: ReqRef#CodeRef =>
            codeRefs += a.value

          case a: Issue#Issue =>
            if (reqId     ne null) issuesR = issuesR.add(reqId,     a)
            if (reqCodeId ne null) issuesG = issuesG.add(reqCodeId, a)
            go(a.desc)

          case a: TagRef#TagRef =>
            if (reqId ne null) {
              tagsInAllReqText = tagsInAllReqText.add(reqId, a.value)
              if (liveText :: Live)
                tagsInLiveReqText = tagsInLiveReqText.add(reqId, a.value)
            }

          case a: ListMarkup#UnorderedList =>
            a.items foreach go
        }

      go(text)
    }

    // Parse reqs
    p.reqs.reqs.values.foreach {
      case r: GenericReq =>
        scan(r.live, reqId = r.id)(r.title)
    }

    // Parse custom-text-field text
    val customTextFieldText = p.reqText
    val liveTextFields      = p.config.liveCustomTextFields.map(_.id).toSet
    for {
      (tf, textByReqId) ← customTextFieldText
      live              = Live <~ (liveTextFields contains tf)
      (id, txt)         ← textByReqId
    } {
      scan(live, reqId = id)(txt.whole)
    }

    // Parse ReqCode groups
    for (gi <- p.reqCodes.activeGroups)
      scan(Live, reqCodeId = gi.id)(gi.group.title)

    new AtomScan(tagsInLiveReqText, tagsInAllReqText, issuesR, issuesG, codeRefs)
  }
}