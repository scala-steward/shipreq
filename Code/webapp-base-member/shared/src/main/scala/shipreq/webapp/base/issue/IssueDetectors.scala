package shipreq.webapp.base.issue

import japgolly.microlibs.adt_macros.AdtMacros
import shipreq.base.util.Util
import shipreq.webapp.base.data._

object IssueDetectors {
  import IssueDetector.{Increment, Init}

  sealed trait Instance extends IssueDetector

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  case object DetectConflictingTags extends Instance {

    override def init(i: Init): Unit =
      checkReqs(i)

    override def increment(i: Increment): Unit = {
      if (i.eventSummary.tagsChanged)
        i.dirtyAllContent()
      checkReqs(i.init)
    }

    private def checkReqs(i: Init) =
      i.action.foreachDirtyLiveReq(() => reqCheckFn(i))

    private def reqCheckFn(i: Init): Req => Unit = {
      val exclusiveGroups = i.project.config.tags.exclusiveGroups
      val content         = i.project.content
      req => {
        val reqId     = req.id
        val tagIds    = content.reqTags(reqId)
        val conflicts = Util.uniqueDupsNested(tagIds)(exclusiveGroups)
        for (g <- conflicts)
          i.action.add(Issue.ConflictingTags(reqId, g))
      }
    }
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  val all = AdtMacros.adtValues[Instance]
}
