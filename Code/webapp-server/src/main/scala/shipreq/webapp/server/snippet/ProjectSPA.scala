package shipreq.webapp.server.snippet

import net.liftweb.http.S
import net.liftweb.util.Helpers._
import scalaz.syntax.equal._
import scalaz.{-\/, \/, \/-}

import shipreq.base.util._
import shipreq.base.util.log.HasLogger
import shipreq.webapp.base.data._
import shipreq.webapp.base.event._
import shipreq.webapp.base.hash.HashScheme
import shipreq.webapp.base.protocol.{ProjectSPA => SpaFns, _}
import shipreq.webapp.server.app.DI
import shipreq.webapp.server.db.EventDao.EventSeq
import shipreq.webapp.server.lib.Types.ProjectId
import shipreq.webapp.server.lib.{SingleOpStatefulSnippet, Taskman}
import shipreq.webapp.server.protocol._

object ProjectSPA extends DI with HasLogger {

  // ALWAYS use the latest to ensure that all parts of Project are hashed.
  // Alternative hash schemes exist so that Project can evolve without breaking old hashes.
  // New events should NEVER use old hash schemes.
  val hashScheme = HashScheme.latest

  case class State(project: Project, hash: Int, seq: EventSeq)

  sealed trait Result
  case class  Updated(project: Project, ae: ActiveEvent, ve: VerifiedEvent) extends Result
  case class  Failed(reason: String)                                        extends Result
  case object NoChange                                                      extends Result

  private def applyEvent(e: ActiveEvent, state: State): Result =
    ApplyEvent.untrusted.apply1(e)(state.project) match {
      case \/-(p2) =>
        val h2 = hashScheme hash p2
        if (h2 == state.hash)
          NoChange
        else {
          val ve = VerifiedEvent(hashScheme, h2, e)
          Updated(p2, e, ve)
        }
      case -\/(err) => Failed(err)
    }

  def applyMakeEventResult(r: MakeEvent.Result, state: State): Result =
    r match {
      case MakeEvent.MadeEvent(e) => applyEvent(e, state)
      case MakeEvent.NoChange     => NoChange
      case MakeEvent.Failed(e)    => Failed(e)
    }

  def loadProjectEvents(projectId: ProjectId): (String, Vector[EventSeq]) \/ State = {
    val es = daoProvider.withSession(_.findAllEvents(projectId))
    ApplyEvent.trusted.applyVerified(es.toStream.map(_._2))(Project.empty) match {

      case \/-(p) =>
        val l = es.lastOption
        val hash = l match {
          case Some(x) if x._2.hashScheme ≟ hashScheme => x._2.hash
          case _                                       => hashScheme hash p
        }
        val seq = l.fold(EventSeq(0))(_._1) // If empty next will be 1 instead of 0. Nice to reserve 0 for ApplyTemplates.
        \/-(State(p, hash, seq))

      case -\/(e) =>
        val seqs = es.map(_._1)
        -\/((e, seqs))
    }
  }

  def loadProjectEvents_!(projectId: ProjectId): State =
    loadProjectEvents(projectId) match {
      case \/-(s) => s
      case -\/((err, seqs)) => sys.error(
        s"Error building project #${projectId.value} from DB events: $err"
        + s"\n\nSeqs: ${seqs mkString ", "}"
      )
    }

  val rightUnit = \/-(())

  val noChangeResponse = \/-(Vector.empty[VerifiedEvent])
}


class ProjectSPA(projectId: ProjectId) extends SingleOpStatefulSnippet {
  import ProjectSPA._

  // val project = RequestVars.Project.get.value

  var state = loadProjectEvents_!(projectId)

  private def updateProject(f: Project => MakeEvent.Result): GenericFailure \/ VerifiedEvents =
    applyMakeEventResult(f(state.project), state) match {
      case u: Updated  =>
        applyNewEvent(u).map(_ => Vector1(u.ve))
      case NoChange =>
        noChangeResponse
      case Failed(err) =>
        -\/(GenericFailure(err))
    }

  private def applyNewEvent(u: Updated): GenericFailure \/ Unit = {
    val seq = state.seq.succ
    val s1 = state
    try {
      daoProvider.withSession(_.createEvent(projectId, seq, u.ae, u.ve.projectHash))
      state = State(u.project, u.ve.hash, seq)
      rightUnit
    } catch {
      case t: Throwable =>
        val msg =
          s"""
             |Error saving new event ${u.ae} with hash ${u.ve.hash} to project ${projectId.value} with seq $seq.
             |Previous state in memory has hash ${s1.hash} and seq ${s1.seq}.
           """.stripMargin
        log.error(t, msg)
        taskman ! Taskman.errorMsg(t, S.request.toOption.map(_.uri), msg)
        -\/(GenericFailure("Error occurred writing change to database."))
    }
  }

  val spaFns = {
    val projectInit = ServerProtocol.remoteFn(ProjectInit)(
      _ => \/-(state.project))

    val customReqTypeCrud = ServerProtocol.remoteFn(CustomReqTypeCrud)(req =>
      updateProject(MakeEvent.customReqTypeCrud(req, _)))

    val reqTypeImplicationMod = ServerProtocol.remoteFn(ReqTypeImplicationMod)(req =>
      updateProject(_ => MakeEvent.reqTypeImplicationMod(req)))

    val customIssueTypeCrud = ServerProtocol.remoteFn(CustomIssueTypeCrud)(req =>
      updateProject(MakeEvent.customIssueTypeCrud(req, _)))

    val tagCrud = ServerProtocol.remoteFn(TagCrud.Fn)(req =>
      updateProject(MakeEvent.tagCrud(req, _)))

    val fieldCrud = ServerProtocol.remoteFn(FieldCrud.Fn)(req =>
      updateProject(MakeEvent.fieldCrud(req, _)))

    val fieldMandatorinessMod = ServerProtocol.remoteFn(FieldMandatorinessMod)(req =>
      updateProject(_ => MakeEvent.fieldMandatorinessMod(req)))

    val createContent = ServerProtocol.remoteFn(CreateContentFn)(req =>
      updateProject(MakeEvent.createContent(req, _)))

    val updateContent = ServerProtocol.remoteFn(UpdateContentFn)(req =>
      updateProject(MakeEvent.updateContent(req, _)))

    SpaFns(projectInit     ,
      customIssueTypeCrud  ,
      customReqTypeCrud    ,
      reqTypeImplicationMod,
      fieldMandatorinessMod,
      fieldCrud            ,
      tagCrud              ,
      createContent        ,
      updateContent        )
  }

  override def render =
    "*" #> ServerProtocol.invokeClientHtml(JsEntryPoint.project)(spaFns)
}
