package shipreq.webapp.base.issue

import japgolly.microlibs.adt_macros.AdtMacros
import shipreq.base.util.Util
import shipreq.webapp.base.data._

object IssueDetectors {
  import IssueDetector.{Increment, Init}

  sealed trait Instance extends IssueDetector

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  case object ConflictingTags extends Instance {

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

  case object UninhabitableTagFields extends Instance {

    override def init(i: Init): Unit = {
      val cfg = i.project.config
      for (f <- i.project.config.fields.customTagFields) {
        val isLive        = f.liveExplicitly is Live
        def uninhabitable = !inhabitable(f.tagId, cfg)
        if (isLive && uninhabitable)
          i.action.add(Issue.UninhabitableTagField(f.id))
      }
    }

    override def increment(i: Increment): Unit =
      if (i.eventSummary.tagsChanged || i.eventSummary.customFieldTypes.nonEmpty) {
        i.dirtyAllContent()
        init(i.init)
      }

    private def inhabitable(id: TagId, cfg: ProjectConfig): Boolean = {
      val t      = cfg.tags.tree.need(id)
      val isLive = t.tag.live is Live
      def typeOk = t.tag match {
        case _: ApplicableTag => true
        case _: TagGroup      => t.children.exists(inhabitable(_, cfg))

      }
      isLive && typeOk
    }
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  val all = AdtMacros.adtValues[Instance]
}
