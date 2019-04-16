package shipreq.webapp.server.logic

import java.time.Instant
import scalaz.syntax.monad._
import scalaz.{Monad, \/-, ~>}
import shipreq.webapp.base.data.{Project, ProjectMetaData}
import shipreq.webapp.base.event._
import shipreq.webapp.base.protocol2.HomeSpaProtocols
import shipreq.webapp.base.user._

trait HomeSpaLogic[F[_]] extends HomeSpaLogic.Ajax[F] {
  def initData(user: User): F[HomeSpaProtocols.InitData]
}

object HomeSpaLogic {

  trait Ajax[F[_]] {
    val ajaxCreateProject: HomeSpaProtocols.createProject.ServerSideFnI[F, User]
  }

  val InitProjectEvent = ProjectTemplateApply(ProjectTemplate.default)
  val InitProject      = ApplyNewEvent.mustApply(InitProjectEvent, Project.empty)

  def createProject[D[_]](userId: UserId,
                          name: Project.Name,
                          now: Instant)
                         (implicit db: DB.ForHomeSpa[D], D: Monad[D]): D[ProjectMetaData] =
    db.inDbTransaction(
      for {
        pid ← db.createEmptyProject(userId)
        e1  = ApplyNewEvent.mustApply(ProjectNameSet(name), InitProject.project)
        _   ← db.saveProjectEvents(pid)(
                DB.SaveProjectEventCmd(EventOrd(0), InitProject.event, InitProject.hashRecs) ::
                DB.SaveProjectEventCmd(EventOrd(1), e1.event, e1.hashRecs) ::
                Nil)
      } yield ProjectMetaData(Obfuscators.projectId.obfuscate(pid), name, 0, 0, now, None))

  def apply[D[_], F[_]](implicit db: DB.ForHomeSpa[D],
                        runDB: D ~> F,
                        svr: Server.Algebra[F],
                        D: Monad[D],
                        F: Monad[F]): HomeSpaLogic[F] =
    new HomeSpaLogic[F] {

      override def initData(user: User): F[HomeSpaProtocols.InitData] =
        for {
          p <- runDB(db.getAllProjectMetaDataForUser(user.id))
        } yield HomeSpaProtocols.InitData(user.username, p)

      override val ajaxCreateProject =
        (user, name) => svr.now.flatMap(now => runDB(createProject(user.id, name, now)))
    }
}
