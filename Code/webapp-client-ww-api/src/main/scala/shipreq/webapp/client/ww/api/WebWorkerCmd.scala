package shipreq.webapp.client.ww.api

import boopickle.ConstPickler
import boopickle.DefaultBasic._
import scala.collection.compat.immutable.ArraySeq
import scalaz.\/
import shipreq.base.util.ErrorMsg
import shipreq.webapp.base.data._
import shipreq.webapp.base.data.savedview.ImpGraphConfig
import shipreq.webapp.base.event.{ProjectAndOrd, VerifiedEvent}
import shipreq.webapp.base.text.ProjectText

// Another idea could be to maintain a separate ClientData instance in the WW thread and feed it all the same updates
// that the main thread processes.

sealed abstract class WebWorkerCmd[Result](implicit final val resultPickler: Pickler[Result])

object WebWorkerCmd {

  // Using instead of Unit so that we can define an implicit Pickler here and have it be universally in scope
  case object NoResult

  final case class SetProject(projectAndOrd: ProjectAndOrd) extends WebWorkerCmd[NoResult.type]

  final case class UpdateProject(events: VerifiedEvent.NonEmptySeq) extends WebWorkerCmd[NoResult.type]

  final case class GraphUseCaseStepFlow(id     : UseCaseId,
                                        project: Project,
                                        ctx    : ProjectText.Context) extends WebWorkerCmd[ErrorMsg \/ Svg]

  final case class GraphReqImplications(focus     : ReqId,
                                        filterDead: FilterDead,
                                        imps      : Implications.BiDir,
                                        reqs      : Requirements,
                                        reqTypes  : ReqTypes) extends WebWorkerCmd[ErrorMsg \/ Svg]

  final case class GraphAllImplications(imps      : Implications.BiDir,
                                        reqs      : Requirements,
                                        reqTypes  : ReqTypes,
                                        scope     : Option[Set[ReqId]],
                                        reqColours: Option[Map[ReqId, ArraySeq[Colour]]],
                                        config    : ImpGraphConfig) extends WebWorkerCmd[ErrorMsg \/ Svg]

  object GraphAllImplications {

    def build(project   : Project,
              filterDead: FilterDead,
              scope     : Option[Set[ReqId]],
              config    : ImpGraphConfig): GraphAllImplications = {

      def reqIds = scope.fold(project.content.reqs.idIterator())(_.iterator)
      val reqColours: Option[Map[ReqId, ArraySeq[Colour]]] =
        config.colours match {
          case ImpGraphConfig.Colours.ByReqType =>
            None
          case ImpGraphConfig.Colours.ByTag(tagGroupId) =>
            val tagLookup = project.dataLogic.tagLookup(filterDead)
            val tags      = project.config.tags
            val tagScope  = project.config.tagFieldDistribution(filterDead).inTagGroup(tagGroupId)
            val colourMap =
              reqIds.map { reqId =>
                val colours =
                  tagLookup(reqId).all
                    .iterator
                    .filter(tagScope.contains)
                    .map(tags.needApplicableTag)
                    .map(t => t.colour.getOrElse(Colour.tagDefault).live(t.live))
                    .to(ArraySeq)
                reqId -> colours
              }.toMap
            Some(colourMap)
        }
      apply(
        imps       = project.content.implications,
        reqs       = project.content.reqs,
        reqTypes   = project.config.reqTypes,
        scope      = scope,
        reqColours = reqColours,
        config     = config)
    }
  }

  // ===================================================================================================================

  import shipreq.webapp.base.protocol.binary.v1.BaseData._
  import shipreq.webapp.base.protocol.binary.v1.BaseMemberData1._
  import shipreq.webapp.base.protocol.binary.v1.BaseMemberData2._
  import shipreq.webapp.base.protocol.binary.v1.Rev1._
  import shipreq.webapp.base.protocol.binary.v1.Rev1.SavedViewPicklers._

  implicit val picklerNoResult: Pickler[NoResult.type] =
    ConstPickler(NoResult)

  implicit val picklerSetProject: Pickler[SetProject] =
    transformPickler(SetProject.apply)(_.projectAndOrd)

  implicit val picklerUpdateProject: Pickler[UpdateProject] =
    transformPickler(UpdateProject.apply)(_.events)

  implicit val picklerErrorMsgOrSvg: Pickler[ErrorMsg \/ Svg] =
    pickleDisj

  private implicit val picklerGraphUseCaseStepFlow: Pickler[GraphUseCaseStepFlow] =
    new Pickler[GraphUseCaseStepFlow] {
      override def pickle(a: GraphUseCaseStepFlow)(implicit state: PickleState): Unit = {
        state.pickle(a.id)
        state.pickle(a.project)
        state.pickle(a.ctx)
      }
      override def unpickle(implicit state: UnpickleState): GraphUseCaseStepFlow = {
        val id      = state.unpickle[UseCaseId]
        val project = state.unpickle[Project]
        val ctx     = state.unpickle[ProjectText.Context]
        GraphUseCaseStepFlow(id, project, ctx)
      }
    }

  private implicit val picklerGraphAllImplications: Pickler[GraphAllImplications] =
    new Pickler[GraphAllImplications] {
      override def pickle(a: GraphAllImplications)(implicit state: PickleState): Unit = {
        state.pickle(a.imps)
        state.pickle(a.reqs)
        state.pickle(a.reqTypes)
        state.pickle(a.scope)
        state.pickle(a.reqColours)
        state.pickle(a.config)
      }
      override def unpickle(implicit state: UnpickleState): GraphAllImplications = {
        val imps       = state.unpickle[Implications.BiDir]
        val reqs       = state.unpickle[Requirements]
        val reqTypes   = state.unpickle[ReqTypes]
        val scope      = state.unpickle[Option[Set[ReqId]]]
        val reqColours = state.unpickle[Option[Map[ReqId, ArraySeq[Colour]]]]
        val config     = state.unpickle[ImpGraphConfig]
        GraphAllImplications(imps, reqs, reqTypes, scope, reqColours, config)
      }
    }

  private implicit val picklerGraphReqImplications: Pickler[GraphReqImplications] =
    new Pickler[GraphReqImplications] {
      override def pickle(a: GraphReqImplications)(implicit state: PickleState): Unit = {
        state.pickle(a.focus)
        state.pickle(a.filterDead)
        state.pickle(a.imps)
        state.pickle(a.reqs)
        state.pickle(a.reqTypes)
      }
      override def unpickle(implicit state: UnpickleState): GraphReqImplications = {
        val focus      = state.unpickle[ReqId]
        val filterDead = state.unpickle[FilterDead]
        val imps       = state.unpickle[Implications.BiDir]
        val reqs       = state.unpickle[Requirements]
        val reqTypes   = state.unpickle[ReqTypes]
        GraphReqImplications(focus, filterDead, imps, reqs, reqTypes)
      }
    }

  implicit val picklerCmd: Pickler[WebWorkerCmd[_]] =
    new Pickler[WebWorkerCmd[_]] {
      private[this] final val KeySetProject           = 0
      private[this] final val KeyUpdateProject        = 1
      private[this] final val KeyGraphAllImplications = 2
      private[this] final val KeyGraphReqImplications = 3
      private[this] final val KeyGraphUseCaseStepFlow = 4
      override def pickle(a: WebWorkerCmd[_])(implicit state: PickleState): Unit =
        a match {
          case b: SetProject           => state.enc.writeByte(KeySetProject          ); state.pickle(b)
          case b: UpdateProject        => state.enc.writeByte(KeyUpdateProject       ); state.pickle(b)
          case b: GraphAllImplications => state.enc.writeByte(KeyGraphAllImplications); state.pickle(b)
          case b: GraphReqImplications => state.enc.writeByte(KeyGraphReqImplications); state.pickle(b)
          case b: GraphUseCaseStepFlow => state.enc.writeByte(KeyGraphUseCaseStepFlow); state.pickle(b)
        }
      override def unpickle(implicit state: UnpickleState): WebWorkerCmd[_] =
        state.dec.readByte match {
          case KeySetProject           => state.unpickle[SetProject]
          case KeyUpdateProject        => state.unpickle[UpdateProject]
          case KeyGraphAllImplications => state.unpickle[GraphAllImplications]
          case KeyGraphReqImplications => state.unpickle[GraphReqImplications]
          case KeyGraphUseCaseStepFlow => state.unpickle[GraphUseCaseStepFlow]
        }
    }
}
