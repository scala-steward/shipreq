package shipreq.webapp.client.project.widgets

import japgolly.microlibs.utils.Memo
import japgolly.scalajs.react.Reusability
import shipreq.webapp.base.data._
import shipreq.webapp.base.text.ProjectText
import shipreq.webapp.client.project.lib.DataReusability._

final case class ViewReqDataCache(private[ViewReqDataCache] val project: Project) {

  private[this] val cache: FilterDead => ReqId => ViewReq.Data =
    FilterDead.memo { fd =>
      Memo { reqId =>
        ViewReq.Data.fromProject(reqId, project, fd)
      }
    }

  def apply(fd: FilterDead): ReqId => ViewReq.Data =
    cache(fd)
}

object ViewReqDataCache {
  implicit val reusability: Reusability[ViewReqDataCache] =
    Reusability.byRef || Reusability.derive
}

// =====================================================================================================================

final case class ViewReqCache[Ctx <: ProjectText.Context](dataCache: ViewReqDataCache,
                                                          private[ViewReqCache] val pw: ProjectWidgets[Ctx]) {

  private[this] val cache: FilterDead => ReqId => ViewReq =
    FilterDead.memo { fd =>
      val f = dataCache(fd)
      Memo { reqId =>
        f(reqId)(pw)
      }
    }

  def apply(fd: FilterDead): ReqId => ViewReq =
    cache(fd)
}

object ViewReqCache {
  implicit def reusability[Ctx <: ProjectText.Context]: Reusability[ViewReqCache[Ctx]] =
    Reusability.byRef || Reusability.derive
}
