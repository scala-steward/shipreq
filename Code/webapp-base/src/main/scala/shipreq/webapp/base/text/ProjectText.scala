package shipreq.webapp.base.text

import shipreq.base.util.{Must, UnivEq}
import shipreq.webapp.base.data._

object ProjectText {
  @inline def apply[Out](project: Project, _format: Text.AnyOptional => Out): ProjectText[Out] =
    new ProjectText[Out](project) {
      override val format = _format
    }
}

abstract class ProjectText[Out](project: Project) {
  import UnivEq.{mutableHashMapMemo => memo}

  val format: Text.AnyOptional => Out

  val format1: Text.AnyNonEmpty => Out =
    nev => format(nev.whole)

  private val _reqDesc: Req => Out = {
    case r: GenericReq => format(r.desc)
  }

  val reqDesc: Req => Out = {
    val memo = new scala.collection.mutable.HashMap[Req.Id, Out]
    req => memo.getOrElseUpdate(req.id, _reqDesc(req))
  }

  def reqDescById(id: Req.Id): Must[Out] =
    project.reqs.data.reqM(id) map reqDesc

  private val _customTextField: CustomField.Text.Id => Req.Id => Option[Out] =
    fid => {
      val m = project.reqFieldData.data.text.getOrElse(fid, Map.empty)
      m.get(_) map format1
    }

  val customTextField: CustomField.Text.Id => Req.Id => Option[Out] =
    memo { fid => val g = _customTextField(fid); memo(g) }
}
