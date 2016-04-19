package shipreq.webapp.base.test

import shipreq.base.util.univeq._
import shipreq.webapp.base.data._

object DataTestExt {

  implicit class ProjectTestExt(private val p: Project) extends AnyVal {

    def useCaseStepsDeletableRestorable(liveFilter: Live): Iterator[UseCaseStep.Focus] =
      p.reqs.useCases.imap.valuesIterator
        .filter(_.liveUC :: Live)
        .flatMap { uc =>
          val root = uc.rootStep.id
          uc.stepIterator
            .map(s => p.reqs.useCases.focusStep(s.id))
            .filter(f => f.live :: liveFilter && f.id !=* root)
        }

    def useCaseStepsDeletable = useCaseStepsDeletableRestorable(Live)
    def useCaseStepsRestorable = useCaseStepsDeletableRestorable(Dead)
  }

}
