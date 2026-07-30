package shipreq.webapp.member.project.issue

import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.formula.FormulaEvalCache

final class IssueTracker(val issues : Issues,
                         val project: Project,
                         val formulaEvalCache: FormulaEvalCache) {

  /** This used to be incremental */
  def update(newProject: Project, newFormulaEvalCache: FormulaEvalCache): IssueTracker =
    IssueTracker(newProject, newFormulaEvalCache)
}

object IssueTracker {

  def apply(project: Project, formulaEvalCache: FormulaEvalCache): IssueTracker = {
    val tstateM = new MutableTrackerState

    tstateM.issues ++= project.manualIssues.imap.valuesIterator.map(Issue.ManualIssue)

    val ctx = IssueDetector.Ctx(
      project          = project,
      formulaEvalCache = formulaEvalCache,
      add              = i => tstateM.issues += i,
      foreachLiveReq   = tstateM.dirtyFns.liveReq.add,
      foreachLiveRcg   = tstateM.dirtyFns.liveRcg.add,
      foreachLiveUcs   = tstateM.dirtyFns.liveUcs.add)

    // Run and prepare detectors
    for (d <- IssueDetectors.all)
      d.detect(ctx)

    // Scan project
    tstateM.dirtyFns.liveReq.foreach(project.liveReqIterator())
    tstateM.dirtyFns.liveRcg.foreach(project.content.reqCodes.liveGroups)
    tstateM.dirtyFns.liveUcs.foreach(project.content.reqs.useCases.liveStepIterator())

    buildTracker(project, formulaEvalCache, tstateM)
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  private def buildTracker(newProject: Project, formulaEvalCache: FormulaEvalCache, tstateM: MutableTrackerState): IssueTracker = {
    val issues = Issues(tstateM.issues.result())
    new IssueTracker(issues, newProject, formulaEvalCache)
  }

  private def fuseReduce[A](fs: IterableOnce[() => A => Unit]): A => Unit =
    fs.iterator.map(_()).reduce((x, y) => a => { x(a); y(a) })

  private final class MutableTrackerState {
    val dirtyFns = new MutableDirtyFns
    val issues = Vector.newBuilder[Issue]
  }

  private final class MutableDirtyFnsA[A] {
    private var fns: List[() => A => Unit]     = Nil
    val add        : (() => A => Unit) => Unit = fns ::= _

    def foreach(as: => IterableOnce[A]): Unit =
      if (fns.nonEmpty) {
        val i = as
        if (i.iterator.nonEmpty) {
          val f = fuseReduce(fns)
          i.iterator.foreach(f)
        }
      }
  }

  private final class MutableDirtyFns {
    val liveReq = new MutableDirtyFnsA[Req]
    val liveRcg = new MutableDirtyFnsA[LiveCodeGroup]
    val liveUcs = new MutableDirtyFnsA[UseCaseStep.Focus]
  }
}
