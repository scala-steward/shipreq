package shipreq.webapp.base.data

import shipreq.webapp.base.text.Atom._
import nyaya.util.Multimap
import shipreq.base.util.Direction

final class ReverseDependencies(atomScan: AtomScan, useCases: UseCases) {

  // This is super minimal at the moment. All I need is reverse-lookup for use case steps.

  private var _useCaseStepIdRefs = Multimap.empty[UseCaseStepId, Set, Location]

  // Scan: refs in reqs
  for {
    (src, lds) <- atomScan.contentRefsInReqs.raw
    ld         <- lds.all
  } ld.value match {

    case tgt: ContentRef # UseCaseStepRef =>
      val loc = Location.Req(src, ld.loc)
      _useCaseStepIdRefs = _useCaseStepIdRefs.add(tgt.value, loc)

    case _ =>
  }

  // Scan: refs in RCGs
  for {
    (src, lds) <- atomScan.contentRefsInRcgs.raw
    ld         <- lds.all
  } ld.value match {

    case tgt: ContentRef # UseCaseStepRef =>
      val loc = Location.ReqCodeGroup(src, ld.loc)
      _useCaseStepIdRefs = _useCaseStepIdRefs.add(tgt.value, loc)

    case _ =>
  }

  // Scan: use case step flow
  for {
    dir         <- Direction
    (src, tgts) <- useCases.stepFlow(dir).iterator
    loc          = Location.Req(useCases.stepIndex(src).useCaseId, Location.Text.UseCaseStep(src))
    tgt         <- tgts
  }
    _useCaseStepIdRefs = _useCaseStepIdRefs.add(tgt, loc)

  // ===================================================================================================================

  def useCaseStepId(id: UseCaseStepId): Set[Location] =
    _useCaseStepIdRefs(id)

}
