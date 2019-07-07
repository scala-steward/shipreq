package shipreq.webapp.client.project.app.issues

import shipreq.base.util.Util
import shipreq.webapp.base.UiText
import shipreq.webapp.base.data._
import shipreq.webapp.base.issue.IssueCategory
import shipreq.webapp.base.sort.{Sorter => SorterBase}

object Sorter {
  import SorterBase._

  val Types = new WithTypes[Setup, Row]
  import Types._

  final class Setup(val p: Project)

  private def pubidNormaliser(setup: Setup): Pubid => (Int, Int) =
    DataLogic.pubidSortKeyFn(setup.p.config)

  val pubidSorter = sorter[(Int, Int)](
    prep =
      setup => {
        val n = pubidNormaliser(setup)
        val `n/a` = (-1, -1)
        ;{
          case r: Row.ForReq    => n(r.req.pubid)
          case _: Row.ForRcg
             | _: Row.ForConfig => `n/a`
        }
      },
    sort = SortFn.intPair
  )

  val issueCategorySorter: Sorter = {
    val ordering = Util.enumOrdering(IssueCategory.values.whole)(UiText.Issues.category)
    val sortFn = SortFn.fromOrdering(ordering)
    sorter(_ => _.issue.category, sortFn)
  }

  val issueClassSorter: Sorter =
    sorter(_ => _.issueClassDesc, SortFn.stringNonEmpty)
}
