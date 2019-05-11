package shipreq.webapp.server.logic

import com.typesafe.scalalogging.StrictLogging
import japgolly.univeq._
import java.time.{Duration, Instant}
import scala.util.{Failure, Success}
import scalaz.syntax.monad._
import scalaz.{-\/, BindRec, Monad, \/, \/-, ~>}
import shipreq.base.ops.Trace
import shipreq.base.util._
import shipreq.base.util.JavaTimeHelpers._
import shipreq.webapp.base.data.{Obfuscated, Project, ProjectId, ProjectMetaData}
import shipreq.webapp.base.event.{ProjectAndOrd, VerifiedEvent}
import shipreq.webapp.base.protocol.ProjectSpaProtocols.WsReqRes.EventResult
import shipreq.webapp.base.protocol.ProjectSpaProtocols.{InitAppData, InitPageData, WsReqRes}
import shipreq.webapp.base.protocol._
import shipreq.webapp.base.user.{User, Username}

trait ProjectSpaLogic[F[_]] {
  import ProjectSpaLogic._

  def initPage(projectId: ProjectId, username: Username): F[InitPageData]

  def onConnect(cookies  : Cookie.LookupFn,
                projectId: ProjectId.Public): F[ConnectRejection \/ (WebSocketStatic, WebSocketState[F])]

  def onOpen(static: WebSocketStatic,
             state : WebSocketState[F],
             push  : BinaryData => F[Unit]): F[WebSocketState[F]]

  def onMessage(static : WebSocketStatic,
                msg    : BinaryData,
                respond: BinaryData => F[Throwable \/ Unit],
                onError: MsgError => F[Unit]): F[Unit]

  // Option is used because this is called after onConnect rejection
  // (in which case valid values are never created for the session)
  def onClose(static: Option[WebSocketStatic],
              state : Option[WebSocketState[F]]): F[Unit]
}

object ProjectSpaLogic extends StrictLogging {

  final case class WebSocketStatic(user       : User,
                                   projectId  : ProjectId,
                                   span       : Any,
                                   connectedAt: Instant)

  final case class WebSocketState[F[_]](sub: Option[Redis.Subscription[F]])
  object WebSocketState {
    def empty[F[_]] = apply[F](None)
  }

  sealed trait ConnectRejection
  object ConnectRejection {
    case object NoSession        extends ConnectRejection
    case object AnonymousSession extends ConnectRejection
    case object InvalidProjectId extends ConnectRejection
    case object ProjectNotFound  extends ConnectRejection
    case object AccessDenied     extends ConnectRejection
    implicit def univEq: UnivEq[ConnectRejection] = UnivEq.derive
  }

  sealed trait MsgError
  object MsgError {
    case object DecodingFailure extends MsgError
    final case class RespondError(err: Throwable) extends MsgError
  }

  // ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████

  def apply[D[_], F[_]](implicit
                        D       : Monad[D],
                        F       : Monad[F] with BindRec[F],
                        db      : DB.ForProjectSpa[D],
                        metrics : MetricsLogic[F],
                        redis   : Redis.ProjectAlgebra[F],
                        runDB   : D ~> F,
                        security: Security.Algebra[F],
                        svr     : Server.Time[F],
                        trace   : Trace.Algebra[F]): ProjectSpaLogic[F] = {
    val webSocketHelper = WebSocketServerHelper(ProjectSpaProtocols.WebSocket(Obfuscated(null)))

    val OnConnect  = Monads.FDisj[F, ConnectRejection]
    val OnMsgError = Monads.FDisj[F, MsgError]

    val fUnit = F.pure(())

    import trace.Span

    def getSpan(static: WebSocketStatic): Span =
      static.span.asInstanceOf[Span]

    new ProjectSpaLogic[F] { self =>

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

      override def initPage(pid: ProjectId, username: Username): F[InitPageData] =
        for {
          name <- runDB(db.projectSpaInitPage(pid))
        } yield {
          val pidPub = Obfuscators.projectId.obfuscate(pid)
          InitPageData(username, pidPub, name)
        }

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

      override def onConnect(cookies  : Cookie.LookupFn,
                             projectId: ProjectId.Public) = {
        val C = OnConnect
        import ConnectRejection._

        def main(span: Span): C.Result[(WebSocketStatic, WebSocketState[F])] =
          for {
            pid     <- C.lift(Obfuscators.projectId.deobfuscate(projectId).leftMap(_ => InvalidProjectId))
            session <- C.optionF(security.sessionRestore(cookies), NoSession)
            user    <- C.option(session.authenticatedUser, AnonymousSession)
            _       <- C.rightF(trace.addAttrs(Trace.Attr.ShipReqUserId(user.id.value) ::
                                               Trace.Attr.ShipReqProjectId(pid.value) :: Nil)(span))
            owner   <- C.optionF(security.db.getProjectOwner(pid), ProjectNotFound)
            _       <- C.ensure(user.id ==* owner, AccessDenied)
            now     <- C.rightF(svr.now)
          } yield {
            val static = WebSocketStatic(user, pid, span, now)
            val state  = WebSocketState.empty[F]
            (static, state)
          }

        trace.newSpan("WebSocket")(span =>
          trace.newSubSpan("onConnect", span)(_ =>
            security.protect(
              for {
                (r, dur) ← svr.measureDuration(main(span).value)
                mresult  = r match {
                             case \/-(_)                => "ok"
                             case -\/(NoSession       ) => "NoSession"
                             case -\/(AnonymousSession) => "AnonymousSession"
                             case -\/(InvalidProjectId) => "InvalidProjectId"
                             case -\/(ProjectNotFound ) => "ProjectNotFound"
                             case -\/(AccessDenied    ) => "AccessDenied"
                           }
                _        ← metrics.projectSpaWebSocketConnected(dur, mresult)
              } yield r
            )))
      }

      override def onOpen(static: WebSocketStatic,
                          state: WebSocketState[F],
                          push : BinaryData => F[Unit]) = {
        val span = getSpan(static)
        def main: F[WebSocketState[F]] =
          state.sub match {
            case None =>
              for {
                sub <- redis.subscribe(static.projectId, pushEvent(span, push, _))
              } yield WebSocketState(Some(sub))
            case Some(_) =>
              F.pure(state)
          }
        trace.newSubSpan("onOpen", span)(_ =>
          for {
            (r, dur) <- svr.measureDuration(main)
            _        <- metrics.projectSpaWebSocketOpened(dur)
          } yield r
        )
      }

      override def onClose(staticO: Option[WebSocketStatic],
                           stateO : Option[WebSocketState[F]]) = {

        val main: F[Unit] =
          stateO.flatMap(_.sub).fold(fUnit)(_.unsubscribe)

        staticO match {
          case Some(static) =>
            trace.newSubSpan("onClose", getSpan(static)) { _ =>
              for {
                dur        ← svr.measureDuration_(main)
                now        ← svr.now
                sessionDur = Duration.between(static.connectedAt, now)
                _          ← metrics.projectSpaWebSocketClosed(dur, sessionDur)
              } yield logger.info(s"WebSocket closed after ${sessionDur.conciseDesc}")
            }
          case None =>
            main
        }
      }

      private def pushEvent(span: Span, push: BinaryData => F[Unit], e: VerifiedEvent): F[Unit] =
        pushEvents(span, push, VerifiedEvent.NonEmptySeq.one(e))

      private def pushEvents(span: Span, push: BinaryData => F[Unit], es: VerifiedEvent.NonEmptySeq): F[Unit] =
        trace.newSubSpan("push", span) { _ =>
          for {
            msgBin <- F point BinaryJvm.encode(webSocketHelper.protocolSC)(-\/(es))
            _      <- metrics.projectSpaWebSocketPush(msgBin.length)
            _      <- push(msgBin)
          } yield ()
        }

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // Message responding

      override def onMessage(static : WebSocketStatic,
                             msg    : BinaryData,
                             respond: BinaryData => F[Throwable \/ Unit],
                             onError: MsgError => F[Unit]): F[Unit] = {
        val M = OnMsgError

        val span = getSpan(static)

        def body(implicit span: Span): F[MsgResult] = {

          def handleError(wsReqRes: FreeOption[WsReqRes], err: MsgError): F[MsgResult] =
            onError(err) >> F.point {
              err match {
                case MsgError.DecodingFailure => logger.warn(s"Failed to decode message: ${msg.describe(1000)}")
                case MsgError.RespondError(e) => logger.error(s"Error sending response.", e)
              }
              new MsgResult(wsReqRes, -1L)
            }

          parseMsg(msg) match {
            case \/-((reqId, req)) =>
              for {
                _              ← trace.rename("onMessage: " + req.reqRes.name)
                res            ← msgFold(req.reqRes)((req.req, static)): F[req.reqRes.ResponseType]
                protocolAndRes = req.reqRes.protocolRes.andValue(res)
                fullRes        = \/-((reqId, protocolAndRes))
                resBin         = BinaryJvm.encode(webSocketHelper.protocolSC)(fullRes)
                wsReqRes       = FreeOption(req.reqRes)
                result         ← respond(resBin).flatMap {
                                   case \/-(_) => F.point(new MsgResult(wsReqRes, resBin.length))
                                   case -\/(e) => handleError(wsReqRes, MsgError.RespondError(e))
                                 }
              } yield result

            case -\/(e) =>
              handleError(FreeOption.empty, e)
          }
        }

        for {
          (r, dur) ← svr.measureDuration(trace.newSubSpan("onMessage", span)(body(_)))
          pid      = static.projectId.value
          _        ← metrics.projectSpaWebSocketMsg(r.msgType, msg.length, r.bytesOut, dur, r.ok)
          _        ← F.point(logger.info(s"WebSocket for project #$pid processed request in ${dur.conciseDesc}"))
        } yield ()
      }

      private def parseMsg(msg: BinaryData) = {
        BinaryJvm.decode(msg, webSocketHelper.protocolCS) match {
          case Success(r) => \/-(r)
          case Failure(_) => -\/(MsgError.DecodingFailure)
        }
      }

      private type MsgFnIn[I] = (I, WebSocketStatic)
      private type MsgFnOut[O] = F[O]
      private type MsgFn[I, O] = (I, WebSocketStatic) => MsgFnOut[O]
      private type MsgFoldIn[R <: WsReqRes] = MsgFnIn[R#RequestType]
      private type MsgFoldOut[R <: WsReqRes] = MsgFnOut[R#ResponseType]

      private val msgFold = WsReqRes.Fold[MsgFoldIn, MsgFoldOut](
        onInitApp               = onInitApp.tupled,
        onCreateContent         = updateProject (MakeEvent.createContent),
        onUpdateContent         = updateProject (MakeEvent.updateContent),
        onProjectNameSet        = updateProjectI(MakeEvent.projectNameSetFn),
        onUpdateSavedViews      = updateProject (MakeEvent.updateSavedViews),
        onFieldMandatorinessMod = updateProjectI(MakeEvent.fieldMandatorinessMod),
        onReqTypeImplicationMod = updateProjectI(MakeEvent.reqTypeImplicationMod),
        onCustomIssueTypeCrud   = updateProject (MakeEvent.customIssueTypeCrud),
        onCustomReqTypeCrud     = updateProject (MakeEvent.customReqTypeCrud),
        onFieldMod              = updateProject (MakeEvent.fieldCrud),
        onTagMod                = updateProject (MakeEvent.tagCrud),
      )

      private def updateProject[I](mkEvent: (I, Project) => MakeEvent.Result): MsgFnIn[I] => MsgFnOut[EventResult] = input => {
        val i = input._1
        val pid = input._2.projectId
        ProjectUpdater[D, F](pid, mkEvent(i, _))
      }

      private def updateProjectI[I](mkEvent: I => MakeEvent.Result): MsgFnIn[I] => MsgFnOut[EventResult] =
        updateProject((i, _) => mkEvent(i))

      // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
      // InitApp

      private def onInitApp: MsgFn[Unit, ErrorMsg \/ InitAppData] = (_, static) => {
        val pid = static.projectId

        type Result = ErrorMsg \/ InitAppData

        def projectNotFound = -\/(ErrorMsg("Project not found."))

        def step[A](name: String)(f: F[A]): F[A] =
          trace.newSpan(name)(_ => metrics.projectSpaWebSocketStep("load", name)(f))

        def ignoreCache(c: Redis.ProjectCache): F[Result] = {

          def readDb(p: ProjectAndOrd) =
            step("readDb")(
              runDB(
                db.inDbTransaction(for {
                  md <- db.getProjectMetaData(pid)
                  es <- db.getProjectEvents(pid, DB.EventFilter.given(p.ord))
                } yield (es, md))
              ).map {
                case (es, Some(md)) =>
                  // Build outside of DB transaction
                  trace.newSpanImpure("ApplyEvents")(_ =>
                    ApplyEvents.append(pid, p, es).map(InitAppData(_, md)))
                case (_, None) =>
                  projectNotFound
              }
            )

          def writeRedis(i: InitAppData): F[Boolean] =
            step("writeRedis")(
              // TODO Maybe write events instead of snapshot
              redis.writeSnapshot(pid, i.project, VerifiedEvent.Seq.empty)
            )

          for {
            result <- readDb(c.nonEmptyCompleteBuild(pid) getOrElse ProjectAndOrd.empty)
            _      <- result.fold[F[_]](_ => fUnit, writeRedis)
          } yield result
        }

        def useCache(c: Redis.ProjectCache, md: ProjectMetaData): F[Result] =
          step("useCache") {
            F point c.build(pid).map(InitAppData(_, md))
          }

        for {
          cache <- redis.read(pid)
          mdOpt <- runDB(db.getProjectMetaData(pid))
          r     <- mdOpt match {
                     case Some(md) => if (cache.isCompleteTo(md.latestOrd)) useCache(cache, md) else ignoreCache(cache)
                     case None     => F pure projectNotFound
                   }
        } yield r
      }

    } // new ProjectSpaLogic
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  private final class MsgResult(reqType: FreeOption[WsReqRes], len: Long) {
    def msgType : String  = reqType.fold("unknown", _.name)
    def ok      : Boolean = len >= 0
    def bytesOut: Long    = if (ok) len else 0
  }

  private object ProjectUpdater {

    type Result = ErrorMsg \/ VerifiedEvent.Seq

    def apply[D[_], F[_]](pid: ProjectId,
                          mkEvent: Project => MakeEvent.Result)
                         (implicit
                          D       : Monad[D],
                          F       : Monad[F] with BindRec[F],
                          db      : DB.ForProjectSpa[D],
                          metrics : MetricsLogic[F],
                          redis   : Redis.ProjectAlgebra[F],
                          runDB   : D ~> F,
                          security: Security.Algebra[F],
                          trace   : Trace.Algebra[F]): F[Result] = {

      def loop(s: State): F[State \/ Result] = {
        import Status._
        val main: F[State \/ Result] = s.status match {

          case ReadRedis =>
            for {
              r0 <- redis.read(pid)
            } yield {
              val r = r0.filterComplete
              r.build(pid) match {
                case \/-(p) => -\/(s.copy(local = p, redis = r, status = if (r > s.local.ord) WriteDb else ReadDb))
                case -\/(e) => \/-(-\/(e))
              }
            }

          case ReadDb =>
            val p1 = s.local max s.redis.nonEmptyCompleteBuild(pid)
            for {
              newEvents <- runDB(db.getProjectEvents(pid, DB.EventFilter.given(p1.ord)))
            } yield
              ApplyEvents.append(pid, p1, newEvents) match {
                case \/-(p2) => -\/(s.copy(local = p2, status = WriteRedis1))
                case -\/(e)  => \/-(-\/(e))
              }

          case WriteRedis1 =>
            for {
              // TODO Maybe write events instead of snapshot
              ok <- redis.writeSnapshot(pid, s.local, VerifiedEvent.Seq.empty)
            } yield -\/(s.copy(status = if (ok) WriteDb else ReadRedis))

          case WriteDb =>
            mkEvent(s.local.project).flatMap(ApplyNewEvent(_, s.local.project)) match {
              case PotentialChange.Success(updated) =>
                val saveCmd = DB.SaveProjectEventCmd(s.local.nextOrd, updated.event, updated.hashRecs)
                runDB(db.saveProjectEvent(pid, saveCmd)) map {
                  case \/-(ve) =>
                    -\/(s.copy(status = WriteRedis2(VerifiedEvent.Seq.one(ve))))
                  case -\/(_) =>
                    -\/(s.copy(status = ReadRedis))
                }

              case PotentialChange.Unchanged =>
                F pure \/-(\/-(VerifiedEvent.Seq.empty))

              case PotentialChange.Failure(e) =>
                F pure \/-(-\/(ErrorMsg(e)))
            }

          case WriteRedis2(newEvents) =>
            for {
              // TODO Maybe write snapshot instead of events
              ok <- redis.writeEvents(pid, VerifiedEvent.Seq.empty, newEvents)
            } yield \/-(\/-(newEvents))
        }

        trace.newSpan(s.status.name)(_ =>
          metrics.projectSpaWebSocketStep("update", s.status.name)(
            main))
      }

      F.tailrecM(loop)(initialState)
    }

    final case class State(local : ProjectAndOrd,
                           redis : Redis.ProjectCache,
                           status: Status)

    val initialState = State(
      local  = ProjectAndOrd.empty,
      redis  = Redis.ProjectCache.empty,
      status = Status.ReadRedis)

    sealed abstract class Status(final val name: String)
    object Status {
      case object ReadRedis                                      extends Status("ReadRedis")
      case object ReadDb                                         extends Status("ReadDb")
      case object WriteRedis1                                    extends Status("WriteRedis1")
      case object WriteDb                                        extends Status("WriteDb")
      final case class WriteRedis2(newEvents: VerifiedEvent.Seq) extends Status("WriteRedis2")
    }
  }
}
