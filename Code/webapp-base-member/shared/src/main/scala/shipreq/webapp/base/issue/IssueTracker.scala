package shipreq.webapp.base.issue

import japgolly.microlibs.stdlib_ext.StdlibExt._
import shipreq.webapp.base.data._

final class IssueTracker(val issues : Issues,
                         val project: Project) {

  /** This used to be incremental */
  def update(newProject: Project): IssueTracker =
    IssueTracker(newProject)
}

object IssueTracker {

  def apply(project: Project): IssueTracker =
    apply(project, IssueDetectors.all.whole)

  def apply(project: Project, detectors: Vector[IssueDetector]): IssueTracker = {
    val tstateM = new MutableTrackerState(1)

    val ctx = IssueDetector.Ctx(
      project        = project,
      add            = i => tstateM.issues += tstateM.assignId(i),
      foreachLiveReq = tstateM.dirtyFns.liveReq.add,
      foreachLiveRcg = tstateM.dirtyFns.liveRcg.add)

    // Run and prepare detectors
    for (d <- detectors)
      d.detect(ctx)

    // Scan requirements
    for (ff <- tstateM.dirtyFns.liveReq.foreach) {
      val it = project.liveReqIterator()
      if (it.nonEmpty)
        it.foreach(ff())
    }

    // Scan req code groups
    for (ff <- tstateM.dirtyFns.liveRcg.foreach) {
      val it = project.content.reqCodes.liveGroups
      if (it.nonEmpty)
        it.foreach(ff())
    }

    buildTracker(project, detectors, tstateM)
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  private def buildTracker(newProject: Project,
                           detectors : Vector[IssueDetector],
                           tstateM   : MutableTrackerState): IssueTracker = {
    val issues = Issues(tstateM.issues.result())
    new IssueTracker(issues, newProject)
  }

  private def fuse[A](fs: TraversableOnce[() => A => Unit]): Option[() => A => Unit] =
    Option.unless(fs.isEmpty)(() => fs.toIterator.map(_()).reduce((x, y) => a => { x(a); y(a) }))

  private final class MutableTrackerState(firstId: Int) {
    private var _nextId = firstId

    def nextId(): IssueId = {
      val id = IssueId(_nextId)
      _nextId += 1
      id
    }

    def assignId(i: Issue): IssueWithId =
      IssueWithId(nextId(), i)

    val dirtyFns = new MutableDirtyFns

    val issues = Vector.newBuilder[IssueWithId]
  }

  private final class MutableDirtyFnsA[A] {
    private var fns               : List[() => A => Unit]     = Nil
    def nonEmpty                  : Boolean                   = fns.nonEmpty
    val add                       : (() => A => Unit) => Unit = fns ::= _
    def foreach                   : Option[() => A => Unit]   = fuse(fns)
    def +=(f: MutableDirtyFnsA[A]): Unit                      = fns = fns.reverse_:::(f.fns)
  }

  private final class MutableDirtyFns {
    val liveReq = new MutableDirtyFnsA[Req]
    val liveRcg = new MutableDirtyFnsA[LiveCodeGroup]
  }
}
