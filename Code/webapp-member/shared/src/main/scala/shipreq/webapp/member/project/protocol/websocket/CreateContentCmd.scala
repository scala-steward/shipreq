package shipreq.webapp.member.project.protocol.websocket

import shipreq.base.util._
import shipreq.webapp.member.project.data._
import shipreq.webapp.member.project.text.Text

/**
 * A command to create new content in a Project.
 */
sealed trait CreateContentCmd
object CreateContentCmd {

  def empty(reqTypeId: ReqTypeId): CreateContentCmd =
    reqTypeId match {
      case StaticReqType.UseCase => CreateUseCase.empty
      case rt: CustomReqTypeId   => CreateGenericReq.empty(rt)
    }

  def imply(reqTypeId: ReqTypeId, sources: Set[ReqId]): CreateContentCmd = {
    val imps: Direction.Values[Set[ReqId]] =
      Direction.Values {
        case Backwards => sources
        case Forwards  => Set.empty
      }
    reqTypeId match {
      case StaticReqType.UseCase => CreateUseCase.empty.copy(imps = imps)
      case rt: CustomReqTypeId   => CreateGenericReq.empty(rt).copy(imps = imps)
    }
  }

  final case class CreateGenericReq(codes        : Set[ReqCode.Value],
                                    customNumbers: Map[CustomField.Number.Id, Double],
                                    customText   : Map[CustomField.Text.Id, Text.CustomTextField.NonEmptyText],
                                    imps         : Direction.Values[Set[ReqId]],
                                    reqType      : CustomReqTypeId,
                                    tags         : Set[ApplicableTagId],
                                    title        : Text.GenericReqTitle.OptionalText) extends CreateContentCmd {

    def addCustomNumber(f: CustomField.Number.Id, v: Double): CreateGenericReq =
      copy(customNumbers = customNumbers.updated(f, v))

    def addCustomNumber(f: CustomField.Number.Id, v: Option[Double]): CreateGenericReq =
      v.fold(this)(addCustomNumber(f, _))

    def addCustomText(f: CustomField.Text.Id, t: Text.CustomTextField.NonEmptyText): CreateGenericReq =
      copy(customText = customText.updated(f, t))

    def addCustomText(f: CustomField.Text.Id, t: Text.CustomTextField.OptionalText): CreateGenericReq =
      NonEmptyArraySeq.maybe(t, this)(addCustomText(f, _))

    def addImps(d: Direction, add: Set[ReqId]): CreateGenericReq =
      copy(imps = imps.mod(d, add ++ _))

    def addTags(add: Set[ApplicableTagId]): CreateGenericReq =
      copy(tags = add ++ tags)
  }

  object CreateGenericReq {
    def empty(reqType: CustomReqTypeId): CreateGenericReq =
      apply(Set.empty, UnivEq.emptyMap, UnivEq.emptyMap, Direction.Values.both(Set.empty), reqType, Set.empty, ArraySeq.empty)
  }

  // -------------------------------------------------------------------------------------------------------------------

  final case class CreateUseCase(codes        : Set[ReqCode.Value],
                                 customNumbers: Map[CustomField.Number.Id, Double],
                                 customText   : Map[CustomField.Text.Id, Text.CustomTextField.NonEmptyText],
                                 imps         : Direction.Values[Set[ReqId]],
                                 tags         : Set[ApplicableTagId],
                                 title        : Text.UseCaseTitle.OptionalText) extends CreateContentCmd {

    def addCustomNumber(f: CustomField.Number.Id, v: Double): CreateUseCase =
      copy(customNumbers = customNumbers.updated(f, v))

    def addCustomNumber(f: CustomField.Number.Id, v: Option[Double]): CreateUseCase =
      v.fold(this)(addCustomNumber(f, _))

    def addCustomText(f: CustomField.Text.Id, t: Text.CustomTextField.NonEmptyText): CreateUseCase =
      copy(customText = customText.updated(f, t))

    def addCustomText(f: CustomField.Text.Id, t: Text.CustomTextField.OptionalText): CreateUseCase =
      NonEmptyArraySeq.maybe(t, this)(addCustomText(f, _))

    def addImps(d: Direction, add: Set[ReqId]): CreateUseCase =
      copy(imps = imps.mod(d, add ++ _))

    def addTags(add: Set[ApplicableTagId]): CreateUseCase =
      copy(tags = add ++ tags)
  }

  object CreateUseCase {
    def empty: CreateUseCase =
      apply(Set.empty, UnivEq.emptyMap, UnivEq.emptyMap, Direction.Values.both(Set.empty), Set.empty, ArraySeq.empty)
  }

  // -------------------------------------------------------------------------------------------------------------------

  final case class CreateCodeGroup(code : ReqCode.Value,
                                   title: Text.CodeGroupTitle.OptionalText) extends CreateContentCmd

  // ===================================================================================================================
  object CodecsV5 {
    import boopickle.DefaultBasic._
    import shipreq.webapp.base.protocol.binary.v1.BaseData._
    import shipreq.webapp.member.project.protocol.binary.v1.BaseMemberData1._
    import shipreq.webapp.member.project.protocol.binary.v1.BaseMemberData2._
    import shipreq.webapp.member.project.protocol.binary.v1.Rev6.AtomPicklers.instances._
    import shipreq.webapp.member.project.protocol.binary.v2.Rev1._
    // REMEMBER: Don't forget to increment `CodecsVn` if you change these

    private implicit val picklerCreateGenericReq: Pickler[CreateGenericReq] =
      new Pickler[CreateGenericReq] {
        override def pickle(a: CreateGenericReq)(implicit state: PickleState): Unit = {
          writeVersion(1)
          state.pickle(a.codes)
          state.pickle(a.customNumbers)
          state.pickle(a.customText)
          state.pickle(a.imps)
          state.pickle(a.reqType)
          state.pickle(a.tags)
          state.pickle(a.title)
        }
        override def unpickle(implicit state: UnpickleState): CreateGenericReq =
          readByVersion(1) {
            case 0 =>
              val codes         = state.unpickle[Set[ReqCode.Value]]
              val customNumbers = Map.empty[CustomField.Number.Id, Double]
              val customText    = state.unpickle[Map[CustomField.Text.Id, Text.CustomTextField.NonEmptyText]]
              val imps          = state.unpickle[Direction.Values[Set[ReqId]]]
              val reqType       = state.unpickle[CustomReqTypeId]
              val tags          = state.unpickle[Set[ApplicableTagId]]
              val title         = state.unpickle[Text.GenericReqTitle.OptionalText]
              CreateGenericReq(codes, customNumbers, customText, imps, reqType, tags, title)
            case 1 =>
              val codes         = state.unpickle[Set[ReqCode.Value]]
              val customNumbers = state.unpickle[Map[CustomField.Number.Id, Double]]
              val customText    = state.unpickle[Map[CustomField.Text.Id, Text.CustomTextField.NonEmptyText]]
              val imps          = state.unpickle[Direction.Values[Set[ReqId]]]
              val reqType       = state.unpickle[CustomReqTypeId]
              val tags          = state.unpickle[Set[ApplicableTagId]]
              val title         = state.unpickle[Text.GenericReqTitle.OptionalText]
              CreateGenericReq(codes, customNumbers, customText, imps, reqType, tags, title)
        }
      }

    private implicit val picklerCreateUseCase: Pickler[CreateUseCase] =
      new Pickler[CreateUseCase] {
        override def pickle(a: CreateUseCase)(implicit state: PickleState): Unit = {
          writeVersion(1)
          state.pickle(a.codes)
          state.pickle(a.customNumbers)
          state.pickle(a.customText)
          state.pickle(a.imps)
          state.pickle(a.tags)
          state.pickle(a.title)
        }
        override def unpickle(implicit state: UnpickleState): CreateUseCase =
          readByVersion(1) {
            case 0 =>
              val codes         = state.unpickle[Set[ReqCode.Value]]
              val customNumbers = Map.empty[CustomField.Number.Id, Double]
              val customText    = state.unpickle[Map[CustomField.Text.Id, Text.CustomTextField.NonEmptyText]]
              val imps          = state.unpickle[Direction.Values[Set[ReqId]]]
              val tags          = state.unpickle[Set[ApplicableTagId]]
              val title         = state.unpickle[Text.UseCaseTitle.OptionalText]
              CreateUseCase(codes, customNumbers, customText, imps, tags, title)
            case 1 =>
              val codes         = state.unpickle[Set[ReqCode.Value]]
              val customNumbers = state.unpickle[Map[CustomField.Number.Id, Double]]
              val customText    = state.unpickle[Map[CustomField.Text.Id, Text.CustomTextField.NonEmptyText]]
              val imps          = state.unpickle[Direction.Values[Set[ReqId]]]
              val tags          = state.unpickle[Set[ApplicableTagId]]
              val title         = state.unpickle[Text.UseCaseTitle.OptionalText]
              CreateUseCase(codes, customNumbers, customText, imps, tags, title)
        }
      }

    private implicit val picklerCreateCodeGroup: Pickler[CreateCodeGroup] =
      new Pickler[CreateCodeGroup] {
        override def pickle(a: CreateCodeGroup)(implicit state: PickleState): Unit = {
          state.pickle(a.code)
          state.pickle(a.title)
        }
        override def unpickle(implicit state: UnpickleState): CreateCodeGroup = {
          val code  = state.unpickle[ReqCode.Value]
          val title = state.unpickle[Text.CodeGroupTitle.OptionalText]
          CreateCodeGroup(code, title)
        }
      }

    implicit val picklerCreateContentCmd: Pickler[CreateContentCmd] =
      new Pickler[CreateContentCmd] {
        private[this] final val KeyCreateCodeGroup  = 'c'
        private[this] final val KeyCreateGenericReq = 'g'
        private[this] final val KeyCreateUseCase    = 'u'
        override def pickle(a: CreateContentCmd)(implicit state: PickleState): Unit =
          a match {
            case b: CreateCodeGroup  => state.enc.writeByte(KeyCreateCodeGroup ); state.pickle(b)
            case b: CreateGenericReq => state.enc.writeByte(KeyCreateGenericReq); state.pickle(b)
            case b: CreateUseCase    => state.enc.writeByte(KeyCreateUseCase   ); state.pickle(b)
          }
        override def unpickle(implicit state: UnpickleState): CreateContentCmd =
          state.dec.readByte match {
            case KeyCreateCodeGroup  => state.unpickle[CreateCodeGroup]
            case KeyCreateGenericReq => state.unpickle[CreateGenericReq]
            case KeyCreateUseCase    => state.unpickle[CreateUseCase]
          }
      }
  }
}
