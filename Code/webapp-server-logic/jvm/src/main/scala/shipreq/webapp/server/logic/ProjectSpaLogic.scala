package shipreq.webapp.server.logic

import japgolly.univeq._
import scalaz.{Monad, \/, ~>}
import shipreq.base.util.{BinaryData, Monads}
import shipreq.webapp.base.data.ProjectId
import shipreq.webapp.base.user.User

trait ProjectSpaLogic[F[_]] {
  import ProjectSpaLogic._

  def onConnect(cookies: Cookie.LookupFn,
                projectId: ProjectId.Public): F[ConnectRejection \/ (WebSocketStatic, WebSocketState)]

//  final type FRes[R <: ReqRes] = F[Res[R]]
//  val onRequest: ReqRes.Fold[Req, FRes]

  def onMessage(static: WebSocketStatic)
               (state : WebSocketState,
                msg   : BinaryData): F[(BinaryData, Option[WebSocketState])]
}

object ProjectSpaLogic {

  sealed trait ConnectRejection
  object ConnectRejection {
    case object NoSession        extends ConnectRejection
    case object AnonymousSession extends ConnectRejection
    case object InvalidProjectId extends ConnectRejection
    case object ProjectNotFound  extends ConnectRejection
    case object AccessDenied     extends ConnectRejection
  }

  final case class WebSocketStatic(user: User, projectId: ProjectId)

  final case class WebSocketState()
  object WebSocketState {
    val empty = apply()
  }

//  final case class Req[R <: ReqRes](req: R#RequestType, state: WebSocketState, user: User)
//  final case class Res[R <: ReqRes](res: R#ResponseType, stateUpdate: Option[WebSocketState])



  def apply[D[_], F[_]](implicit
                        D: Monad[D],
                        F: Monad[F],
                        db: DB.ForProjectSpa[D],
                        runDB: D ~> F,
                        security: Security.Algebra[F]): ProjectSpaLogic[F] = {

    val OnConnect = Monads.FDisj[F, ConnectRejection]

    new ProjectSpaLogic[F] { self =>

      override def onConnect(cookies: Cookie.LookupFn,
                             projectId: ProjectId.Public) = {
        val C = OnConnect
        import ConnectRejection._

        val main: C.Result[(WebSocketStatic, WebSocketState)] =
          for {
            pid     <- C.lift(Obfuscators.projectId.deobfuscate(projectId).leftMap(_ => InvalidProjectId))
            session <- C.optionF(security.sessionRestore(cookies), NoSession)
            user    <- C.option(session.authenticatedUser, AnonymousSession)
            p       <- C.optionF(runDB(db.getProjectHeader(pid)), ProjectNotFound)
            _       <- C.ensure(user.id ==* p.userId, AccessDenied)
          } yield {
            val static = WebSocketStatic(user, pid)
            val state  = WebSocketState.empty
            (static, state)
          }

        security.protect(main.value)
      }

      override def onMessage(static: WebSocketStatic)
                            (state : WebSocketState,
                             msg   : BinaryData) = {
        ???
      }

//      override val onRequest = ReqRes.Fold[Req, FRes](
//        onInitApp               = onInitApp,
//        onProjectNameSet        = ???,
//        onFieldMandatorinessMod = ???,
//        onReqTypeImplicationMod = ???,
//        onCreateContent         = ???,
//        onUpdateContent         = ???,
//        onUpdateSavedViews      = ???,
//      )
    }
  }
}
