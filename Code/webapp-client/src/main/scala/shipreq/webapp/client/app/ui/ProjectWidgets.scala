package shipreq.webapp.client.app.ui

import japgolly.scalacss.ScalaCssReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalaz.Memo
import shipreq.base.util.UnivEq
import shipreq.webapp.base.data._
import shipreq.webapp.client.lib.ui.UI
import shipreq.webapp.client.app.ui.Style.{widgets => *}

final class ProjectWidgets(project: Project) {

  type Widget = ReactComponentC.ConstProps[Unit, Unit, Unit, TopNode]

  private def memo[A: UnivEq](n: String, f: A => ReactTag): A => Widget =
    Memo.mutableHashMapMemo((a: A) => ReactComponentB.static(n, f(a)).buildU)

  val pubId = memo[Pubid]("ID", pubid =>
    UI.must(project.reqType(pubid.reqTypeId))(rt =>
      <.span(s"${rt.mnemonic.value}-${pubid.pos.value}")
    ))

  val reqType = memo[ReqType.Id]("ReqType", id =>
    UI.must(project.reqType(id))(rt =>
      <.span(
        ^.title := rt.name,
        s"${rt.mnemonic.value}")
    ))

  val tag = memo[ApplicableTag.Id]("Tag", id =>
    UI.must(project.atag(id))(tag =>
      <.span(
        *.tag,
        ^.title := tag.name,
        tag.key.value
      )
    ))

  def tagList(tags: List[ApplicableTag.Id]): ReactElement =
    <.div(tags.map(id => tag(id)(): TagMod): _*)
}
