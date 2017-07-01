package shipreq.webapp.server.logic

import java.time.{Duration, Instant}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.immutable.SortedMap
import scalaz.{Name, NaturalTransformation, \/, ~>}
import scalaz.syntax.monad._
import shipreq.base.util._
import shipreq.taskman.api.{Msg, MsgId, MsgStatus, TaskmanApi}
import shipreq.webapp.base.data._
import shipreq.webapp.base.event._
import shipreq.webapp.base.user._
import shipreq.webapp.base.hash.HashRec
import shipreq.webapp.base.protocol.ServerSideProc
import shipreq.webapp.base.test.WebappTestUtil._
import shipreq.webapp.server.ServerConfig

object MockDb {
  final case class Entry(projectId    : ProjectId,
                         userId       : UserId,
                         events       : VerifiedEvent.Seq,
                         createdAt    : Instant,
                         lastUpdatedAt: Option[Instant]) {

    lazy val project: Project =
      ApplyEvent.trusted.applyVerified(events.eventVector)(Project.empty).needRight

    lazy val projectMetaData: ProjectMetaData =
      ProjectMetaData(id            = ProjectId.Extern(projectId),
                      name          = project.name,
                      eventCount    = events.eventVector.length,
                      reqCount      = project.reqs.size,
                      createdAt     = createdAt,
                      lastUpdatedAt = lastUpdatedAt)

    lazy val projectLoad: DB.ProjectEvents =
      (SortedMap.empty: DB.ProjectEvents) ++ events.iterator
  }
}

final class MockDb(now: Name[Instant]) extends DB.Algebra[Name] {

  var prevTokenId = 0
  private def nextToken(): SecurityToken = {
    prevTokenId += 1
    prevToken()
  }

  def prevToken(): SecurityToken = {
    assert(prevTokenId > 0)
    SecurityToken(s"[token-$prevTokenId]")
  }

  def assertTokensIssued(expect: Int): Unit =
    assertEq("assertTokensIssued", prevTokenId, expect)

  var userPlaceholders = Map.empty[EmailAddr, DB.UserRegistration]
  override def createUserPlaceholder(e: EmailAddr) =
    now.map { n =>
      assert(!userPlaceholders.contains(e))
      val t = nextToken()
      userPlaceholders += e -> DB.UserRegistration.Pending(UserId(prevTokenId), t, n)
      t
    }

  override def getUserRegistration(e: EmailAddr) = Name[Option[DB.UserRegistration]] {
    userPlaceholders get e
  }

  override def updateUserRegistrationToken(id: UserId) =
    now.map { n =>
      val (e, u) = userPlaceholders.iterator.find(_._2.id ==* id) getOrElse sys.error("User not found")
      u match {
        case r: DB.UserRegistration.Complete => fail("Registration complete")
        case r: DB.UserRegistration.Pending =>
          val t = nextToken()
          userPlaceholders += e -> r.copy(token = t, tokenSentAt = n)
          t
      }
    }

  private var projects: IMap[ProjectId, MockDb.Entry] =
    IMap.empty(_.projectId)

  def addProject(projectId: ProjectId, userId: UserId)(events: Event*): Unit = {
    val ves = VerifiedEvent.Seq(EventOrd(1), verifyEvents(Project.empty)(events: _*))
    val now = Instant.now()
    val mde = MockDb.Entry(projectId, userId, ves, now, Some(now))
    projects = projects.add(mde)
  }

  override def createEmptyProject(id: UserId) = Name[ProjectId] {
    val pid = ProjectId(1 + projects.underlyingMap.keysIterator.map(_.value).foldLeft(0L)(_ max _))
    addProject(pid, id)()
    pid
  }

  override def getAllProjectMetaDataForUser(id: UserId) = Name[List[ProjectMetaData]] {
    projects.valuesIterator
      .filter(_.userId ==* id)
      .map(_.projectMetaData)
      .toList
  }

  var loadProjectHeaderLog = Vector.empty[ProjectId]
  override def getProjectHeader(id: ProjectId) = Name[Option[ProjectHeader]] {
    loadProjectHeaderLog :+= id
    projects.get(id).map(e => ProjectHeader(e.userId, e.project.name))
  }

  var loadProjectMetaDataLog = Vector.empty[ProjectId]
  override def getProjectMetaData(id: ProjectId) = Name[Option[ProjectMetaData]] {
    loadProjectMetaDataLog :+= id
    projects.get(id).map(_.projectMetaData)
  }

  var loadProjectLog = Vector.empty[ProjectId]
  override def getAllProjectEvents(id: ProjectId) = Name[DB.ProjectEvents] {
    loadProjectLog :+= id
    projects.need(id).projectLoad
  }

  override def saveProjectEvent(id: ProjectId)(ord: EventOrd, e: ActiveEvent, hrs: HashRec.Collection) = Name[Option[Throwable]] {
    val entry = projects.need(id)
    def update(events: VerifiedEvent.Seq): Unit =
      projects = projects + entry.copy(events = events, lastUpdatedAt = Some(Instant.now()))
    val ve = verifyEvent(entry.project, e)
    entry.events match {
      case ves: VerifiedEvent.NonEmptySeq =>
        if (ord.immediatelyFollows(ves.lastOrd)) {
          update(ves.copy(events = ves.events :+ ve))
          None
        } else
          Some(new RuntimeException(s"$ord doesn't follow ${ves.lastOrd}"))
      case VerifiedEvent.EmptySeq =>
        update(VerifiedEvent.NonEmptySeq.one(ord, ve))
        None
    }
  }

  override def inDbTransaction[A](f: Name[A]) =
    f
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

final class MockServer extends Server.Algebra[Name] {
  private var prevFn = 0
  private var fns: Map[String, Any] =
    UnivEq.emptyMap

  override def createServerSideProc(p: ServerSideProc.Protocol)(localFn: p.Input => Name[p.Response]) = Name[p.Instance] {
    prevFn += 1
    val key = prevFn.toString
    fns = fns.updated(key, localFn)
    ServerSideProc(key, p)
  }

  def run(p: ServerSideProc)(i: p.protocol.Input): p.protocol.Response = {
    val f = fns(p.key).asInstanceOf[p.protocol.Input => Name[p.protocol.Response]]
    f(i).value
  }

  var clock = Instant.now()
  override val now = Name(clock)

  private def durationBorder(duration: Duration, tolerance: Duration = Duration.ofSeconds(2)): Validity => Duration = {
    case Valid   => duration minus tolerance
    case Invalid => duration plus tolerance
  }

  def forwardTimeToEndOfWindow(w: Duration, v: Validity): Unit =
    clock = clock plus durationBorder(w)(v)

  var onDelay = List.empty[() => Unit]
  override def delay[A](f: Name[A], d: Duration) = Name[A] {
    clock = clock plus d
    onDelay match {
      case Nil    => ()
      case h :: t => onDelay = t; h()
    }
    f.value
  }

  var forked = Vector.empty[Name[Any]]
  override def fork[A](f: Name[A]) = Name[Unit] {
    forked :+= f
  }
  def runForked(): Unit = {
    forked.foreach(_.value)
    forked = Vector.empty
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

final class MockTaskman extends TaskmanApi[Name] {
  private var prevMsgId = 0L

  override def cfgPut(key: String, value: String) = Name[Unit] {
    ()
  }

  var msgs = Vector.empty[(MsgId, Msg)]
  override def submitMsg(m: Msg) = Name[MsgId] {
    prevMsgId += 1
    val id = MsgId(prevMsgId)
    msgs :+= (id, m)
    id
  }

  override def queryMsgStatus(id: MsgId) = Name[Option[MsgStatus]] {
    None
  }

  def assertSubmitted(expect: Int): Unit =
    if (msgs.length !=* expect)
      fail(s"Expected $expect Taskman tasks submitted, got ${msgs.length}: ${msgs.mkString(", ")}")

  def assertLastSubmitted[A](pf: PartialFunction[Msg, A]): A =
    if (msgs.isEmpty)
      fail("No tasks submitted.")
    else
      pf.lift(msgs.last._2) getOrElse
        fail(s"Unexpected Taskman task submitted: ${msgs.last._2}")

//  def assertSubmitted(msg: Msg*): Unit =
//    assertEq(msg.toVector, tasksSubmitted.map(_._2))
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

final class MockSecurity extends Security.Algebra[Name] {

  override def protect[A](vulnerable: Name[A]): Name[A] =
    vulnerable

  override def attemptLogin(user: Username \/ EmailAddr, password: PlainTextPassword) = Name[Permission] {
    Deny
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object MockInterpreters {
  import JavaTimeHelpers._

  val config = ServerConfig(
    supportEmailAddress        = "test@shipreq.com",
    baseUrl                    = Url.Absolute.Base("https://test.shipreq.com"),
    attackFrustrationDelay     = 1 hour,
    confirmationTokenLength    = 8,
    confirmationTokenLifespan  = 7 days,
    passwordResetTokenLifespan = 4 days,
    allowRegister              = Allow,
    taskmanSchema              = "test_taskman",
    initTaskmanOnBoot          = false,
    initTaskmanRetry           = RetryCriteria(2 hour, Some(666)),
    flashVarTTL                = 100 days)
}

class MockInterpreters(modCfg: ServerConfig => ServerConfig = Identity[ServerConfig]) {
  implicit val config     = modCfg(MockInterpreters.config)
  implicit val storeMap   = new ConcurrentHashMap(): ProjectServer.StoreMap[Name, ConcurrentHashMap]
  implicit val store      = Store.Algebra.concurrentHashMap(storeMap): ProjectServer.StoreAlgebra[Name]
  implicit val svr        = new MockServer
  implicit val db         = new MockDb(svr.now)
  implicit val security   = new MockSecurity
  implicit val taskman    = new MockTaskman
  implicit val nameToName = NaturalTransformation.refl[Name]
}
