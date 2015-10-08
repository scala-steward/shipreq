package shipreq.webapp.base.test

import monocle._
import monocle.std.{some => atSome}
import shipreq.webapp.base.data._
import shipreq.webapp.base.text.Text.ReqCodeGroupTitle
import shipreq.webapp.base.util.Optics

object TestOptics {

  val customReqTypesLive: Traversal[Project, Live] =
    Project.customReqTypes ^|->>
    Optics.imapTraversal   ^|->
    CustomReqType.live


  import ReqCode._

  val reqCodeDataDeadGroup = Lens[Data, DeadGroup](_.deadGroup)(dg => {
    case d: Inactive    => d.copy(deadGroup = dg)
    case d: ActiveReq   => d.copy(deadGroup = dg)
    case d: ActiveGroup => d
  })

  private val reqCodeDataDeadGroupSome = reqCodeDataDeadGroup ^<-? atSome

  val reqCodeDataDeadGroupId: Optional[Data, ReqCodeId] =
    reqCodeDataDeadGroupSome ^|-> ReqCodeGroup.AndId.id

  val reqCodeDataDeadGroupGroup: Optional[Data, ReqCodeGroup] =
    reqCodeDataDeadGroupSome ^|-> ReqCodeGroup.AndId.group

  val reqCodeDataDeadGroupTitle: Optional[Data, ReqCodeGroupTitle.OptionalText] =
    reqCodeDataDeadGroupGroup ^|-> ReqCodeGroup.title

  val reqCodeDataActiveGroup = Prism[Data, ActiveGroup]({
    case d: ActiveGroup => Some(d)
    case _              => None
  })(d => d)

  val reqCodeDataActiveGroupTitle: Optional[Data, ReqCodeGroupTitle.OptionalText] =
    reqCodeDataActiveGroup ^|-> ActiveGroup.group ^|-> ReqCodeGroup.title

  val reqCodeDataActiveId = Optional[Data, ReqCodeId]({
    case d: ActiveReq   => Some(d.id)
    case d: ActiveGroup => Some(d.id)
    case d: Inactive    => None
  })(n => {
    case d: ActiveReq   => d.copy(id = n)
    case d: ActiveGroup => ActiveGroup.id.set(n)(d)
    case d: Inactive    => d
  })

  val reqCodeDataReqInactive = Lens[Data, ReqInactive](_.reqInactive)(n => {
    case d: ActiveReq   => d.copy(reqInactive = n)
    case d: ActiveGroup => d.copy(reqInactive = n)
    case d: Inactive    => d.copy(reqInactive = n)
  })

  val reqCodeTrieFixK = Trie.fixk
  val reqCodeTrieValueTraversal: Traversal[Trie, Data] =
    PTraversal.fromTraverse[reqCodeTrieFixK.Trie, Data, Data](reqCodeTrieFixK.traverseTrie)
}
