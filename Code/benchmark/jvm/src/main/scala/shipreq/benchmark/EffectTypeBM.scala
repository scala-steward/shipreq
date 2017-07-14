package shipreq.benchmark

import japgolly.microlibs.stdlib_ext.StdlibExt._
import java.time.Instant
import org.openjdk.jmh.annotations._
import monix.eval.Coeval
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}
import scalaz.effect.IO
import scalaz.Free.Trampoline
import scalaz.std.function.function0Instance
import scalaz.syntax.monad._
import scalaz.{Monad, Name, \/}
import shipreq.base.util._
import shipreq.webapp.base.Urls
import shipreq.webapp.base.data.{ProjectId, SecurityToken}
import shipreq.webapp.base.user._
import shipreq.webapp.server.ServerConfig
import shipreq.webapp.server.logic._

/**
  * > sbt
  * > benchmark-jvm/jmh:run -wi 10 -i 10 -f 2 -prof gc EffectTypeBM
  *
  * [info] Benchmark                                                  Mode  Cnt        Score       Error   Units
  * [info] EffectTypeBM.coeval                                       thrpt   20  1151188.129 ± 18185.552   ops/s
  * [info] EffectTypeBM.coeval:·gc.alloc.rate                        thrpt   20      995.179 ±    15.722  MB/sec
  * [info] EffectTypeBM.coeval:·gc.alloc.rate.norm                   thrpt   20     1360.001 ±     0.003    B/op
  * [info] EffectTypeBM.coeval:·gc.churn.PS_Eden_Space               thrpt   20     1039.445 ±   206.388  MB/sec
  * [info] EffectTypeBM.coeval:·gc.churn.PS_Eden_Space.norm          thrpt   20     1421.309 ±   282.740    B/op
  * [info] EffectTypeBM.coeval:·gc.churn.PS_Survivor_Space           thrpt   20        0.077 ±     0.037  MB/sec
  * [info] EffectTypeBM.coeval:·gc.churn.PS_Survivor_Space.norm      thrpt   20        0.106 ±     0.051    B/op
  * [info] EffectTypeBM.coeval:·gc.count                             thrpt   20       84.000              counts
  * [info] EffectTypeBM.coeval:·gc.time                              thrpt   20       70.000                  ms
  * [info] EffectTypeBM.fn0                                          thrpt   20  1548873.762 ± 23614.198   ops/s
  * [info] EffectTypeBM.fn0:·gc.alloc.rate                           thrpt   20      960.908 ±    14.652  MB/sec
  * [info] EffectTypeBM.fn0:·gc.alloc.rate.norm                      thrpt   20      976.001 ±     0.002    B/op
  * [info] EffectTypeBM.fn0:·gc.churn.PS_Eden_Space                  thrpt   20      982.967 ±   202.102  MB/sec
  * [info] EffectTypeBM.fn0:·gc.churn.PS_Eden_Space.norm             thrpt   20      998.847 ±   203.147    B/op
  * [info] EffectTypeBM.fn0:·gc.churn.PS_Survivor_Space              thrpt   20        0.080 ±     0.050  MB/sec
  * [info] EffectTypeBM.fn0:·gc.churn.PS_Survivor_Space.norm         thrpt   20        0.082 ±     0.051    B/op
  * [info] EffectTypeBM.fn0:·gc.count                                thrpt   20       74.000              counts
  * [info] EffectTypeBM.fn0:·gc.time                                 thrpt   20       66.000                  ms
  * [info] EffectTypeBM.io                                           thrpt   20   408958.379 ±  6015.620   ops/s
  * [info] EffectTypeBM.io:·gc.alloc.rate                            thrpt   20     1638.733 ±    24.119  MB/sec
  * [info] EffectTypeBM.io:·gc.alloc.rate.norm                       thrpt   20     6304.004 ±     0.008    B/op
  * [info] EffectTypeBM.io:·gc.churn.PS_Eden_Space                   thrpt   20     1646.687 ±   109.779  MB/sec
  * [info] EffectTypeBM.io:·gc.churn.PS_Eden_Space.norm              thrpt   20     6334.814 ±   411.912    B/op
  * [info] EffectTypeBM.io:·gc.churn.PS_Survivor_Space               thrpt   20        0.087 ±     0.029  MB/sec
  * [info] EffectTypeBM.io:·gc.churn.PS_Survivor_Space.norm          thrpt   20        0.336 ±     0.108    B/op
  * [info] EffectTypeBM.io:·gc.count                                 thrpt   20      160.000              counts
  * [info] EffectTypeBM.io:·gc.time                                  thrpt   20      131.000                  ms
  * [info] EffectTypeBM.name                                         thrpt   20  1425507.654 ± 20138.257   ops/s
  * [info] EffectTypeBM.name:·gc.alloc.rate                          thrpt   20     1105.038 ±    27.812  MB/sec
  * [info] EffectTypeBM.name:·gc.alloc.rate.norm                     thrpt   20     1220.001 ±    39.200    B/op
  * [info] EffectTypeBM.name:·gc.churn.PS_Eden_Space                 thrpt   20     1130.493 ±   189.649  MB/sec
  * [info] EffectTypeBM.name:·gc.churn.PS_Eden_Space.norm            thrpt   20     1247.899 ±   208.748    B/op
  * [info] EffectTypeBM.name:·gc.churn.PS_Survivor_Space             thrpt   20        0.041 ±     0.030  MB/sec
  * [info] EffectTypeBM.name:·gc.churn.PS_Survivor_Space.norm        thrpt   20        0.045 ±     0.033    B/op
  * [info] EffectTypeBM.name:·gc.count                               thrpt   20       47.000              counts
  * [info] EffectTypeBM.name:·gc.time                                thrpt   20       38.000                  ms
  * [info] EffectTypeBM.trampoline                                   thrpt   20  1282149.554 ± 37328.620   ops/s
  * [info] EffectTypeBM.trampoline:·gc.alloc.rate                    thrpt   20     1108.367 ±    32.280  MB/sec
  * [info] EffectTypeBM.trampoline:·gc.alloc.rate.norm               thrpt   20     1360.001 ±     0.003    B/op
  * [info] EffectTypeBM.trampoline:·gc.churn.PS_Eden_Space           thrpt   20     1143.337 ±   203.411  MB/sec
  * [info] EffectTypeBM.trampoline:·gc.churn.PS_Eden_Space.norm      thrpt   20     1405.617 ±   257.204    B/op
  * [info] EffectTypeBM.trampoline:·gc.churn.PS_Survivor_Space       thrpt   20        0.044 ±     0.027  MB/sec
  * [info] EffectTypeBM.trampoline:·gc.churn.PS_Survivor_Space.norm  thrpt   20        0.054 ±     0.034    B/op
  * [info] EffectTypeBM.trampoline:·gc.count                         thrpt   20       48.000              counts
  * [info] EffectTypeBM.trampoline:·gc.time                          thrpt   20       43.000                  ms
  * [success] Total time: 306 s, completed 14/07/2017 1:06:15 PM
  *
  * > sbt -DMODE=release
  * > benchmark-jvm/jmh:run -wi 10 -i 10 -f 2 -prof gc EffectTypeBM
  *
  * [info] Benchmark                                                  Mode  Cnt        Score        Error   Units
  * [info] EffectTypeBM.coeval                                       thrpt   20  1078078.008 ±  62216.662   ops/s
  * [info] EffectTypeBM.coeval:·gc.alloc.rate                        thrpt   20      925.511 ±     45.035  MB/sec
  * [info] EffectTypeBM.coeval:·gc.alloc.rate.norm                   thrpt   20     1352.006 ±     21.382    B/op
  * [info] EffectTypeBM.coeval:·gc.churn.PS_Eden_Space               thrpt   20      952.635 ±     85.311  MB/sec
  * [info] EffectTypeBM.coeval:·gc.churn.PS_Eden_Space.norm          thrpt   20     1392.027 ±    110.710    B/op
  * [info] EffectTypeBM.coeval:·gc.churn.PS_Survivor_Space           thrpt   20        0.105 ±      0.028  MB/sec
  * [info] EffectTypeBM.coeval:·gc.churn.PS_Survivor_Space.norm      thrpt   20        0.152 ±      0.039    B/op
  * [info] EffectTypeBM.coeval:·gc.count                             thrpt   20      104.000               counts
  * [info] EffectTypeBM.coeval:·gc.time                              thrpt   20      114.000                   ms
  * [info] EffectTypeBM.fn0                                          thrpt   20  1283380.667 ± 136173.169   ops/s
  * [info] EffectTypeBM.fn0:·gc.alloc.rate                           thrpt   20      791.595 ±     99.147  MB/sec
  * [info] EffectTypeBM.fn0:·gc.alloc.rate.norm                      thrpt   20      968.001 ±     21.382    B/op
  * [info] EffectTypeBM.fn0:·gc.churn.PS_Eden_Space                  thrpt   20      803.467 ±    153.133  MB/sec
  * [info] EffectTypeBM.fn0:·gc.churn.PS_Eden_Space.norm             thrpt   20      980.091 ±    131.334    B/op
  * [info] EffectTypeBM.fn0:·gc.churn.PS_Survivor_Space              thrpt   20        0.070 ±      0.038  MB/sec
  * [info] EffectTypeBM.fn0:·gc.churn.PS_Survivor_Space.norm         thrpt   20        0.085 ±      0.045    B/op
  * [info] EffectTypeBM.fn0:·gc.count                                thrpt   20       68.000               counts
  * [info] EffectTypeBM.fn0:·gc.time                                 thrpt   20       73.000                   ms
  * [info] EffectTypeBM.io                                           thrpt   20   380031.975 ±  17823.095   ops/s
  * [info] EffectTypeBM.io:·gc.alloc.rate                            thrpt   20     1526.666 ±     71.587  MB/sec
  * [info] EffectTypeBM.io:·gc.alloc.rate.norm                       thrpt   20     6320.004 ±      0.008    B/op
  * [info] EffectTypeBM.io:·gc.churn.PS_Eden_Space                   thrpt   20     1535.751 ±    157.437  MB/sec
  * [info] EffectTypeBM.io:·gc.churn.PS_Eden_Space.norm              thrpt   20     6356.734 ±    551.598    B/op
  * [info] EffectTypeBM.io:·gc.churn.PS_Survivor_Space               thrpt   20        0.128 ±      0.032  MB/sec
  * [info] EffectTypeBM.io:·gc.churn.PS_Survivor_Space.norm          thrpt   20        0.533 ±      0.142    B/op
  * [info] EffectTypeBM.io:·gc.count                                 thrpt   20      134.000               counts
  * [info] EffectTypeBM.io:·gc.time                                  thrpt   20      132.000                   ms
  * [info] EffectTypeBM.name                                         thrpt   20  1325022.413 ±  62553.188   ops/s
  * [info] EffectTypeBM.name:·gc.alloc.rate                          thrpt   20      943.299 ±     44.559  MB/sec
  * [info] EffectTypeBM.name:·gc.alloc.rate.norm                     thrpt   20     1120.001 ±      0.002    B/op
  * [info] EffectTypeBM.name:·gc.churn.PS_Eden_Space                 thrpt   20      953.674 ±    123.595  MB/sec
  * [info] EffectTypeBM.name:·gc.churn.PS_Eden_Space.norm            thrpt   20     1131.582 ±    129.898    B/op
  * [info] EffectTypeBM.name:·gc.churn.PS_Survivor_Space             thrpt   20        0.119 ±      0.029  MB/sec
  * [info] EffectTypeBM.name:·gc.churn.PS_Survivor_Space.norm        thrpt   20        0.141 ±      0.034    B/op
  * [info] EffectTypeBM.name:·gc.count                               thrpt   20      114.000               counts
  * [info] EffectTypeBM.name:·gc.time                                thrpt   20      120.000                   ms
  * [info] EffectTypeBM.trampoline                                   thrpt   20  1318350.505 ±  33385.821   ops/s
  * [info] EffectTypeBM.trampoline:·gc.alloc.rate                    thrpt   20     1138.965 ±     14.645  MB/sec
  * [info] EffectTypeBM.trampoline:·gc.alloc.rate.norm               thrpt   20     1360.001 ±     28.509    B/op
  * [info] EffectTypeBM.trampoline:·gc.churn.PS_Eden_Space           thrpt   20     1130.337 ±    283.824  MB/sec
  * [info] EffectTypeBM.trampoline:·gc.churn.PS_Eden_Space.norm      thrpt   20     1351.553 ±    339.812    B/op
  * [info] EffectTypeBM.trampoline:·gc.churn.PS_Survivor_Space       thrpt   20        0.031 ±      0.032  MB/sec
  * [info] EffectTypeBM.trampoline:·gc.churn.PS_Survivor_Space.norm  thrpt   20        0.038 ±      0.039    B/op
  * [info] EffectTypeBM.trampoline:·gc.count                         thrpt   20       34.000               counts
  * [info] EffectTypeBM.trampoline:·gc.time                          thrpt   20       37.000                   ms
  * [success] Total time: 414 s, completed 14/07/2017 3:33:13 PM
  */
@State(Scope.Benchmark)
class EffectTypeBM {
  import EffectTypeBM._

  def test[F[_]](i: Interpreters[F]): Any =
    DispatchRequests.map(r => i.run(i.dispatcher(r)))

  @Benchmark def coeval     = test(EffectTypeBM.coeval)
  @Benchmark def io         = test(EffectTypeBM.io)
  @Benchmark def fn0        = test(EffectTypeBM.fn0)
//@Benchmark def future     = test(EffectTypeBM.future)
  @Benchmark def name       = test(EffectTypeBM.name)
  @Benchmark def trampoline = test(EffectTypeBM.trampoline)

}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object EffectTypeBM {
  import JavaTimeHelpers._

  implicit val config = ServerConfig(
    baseUrl                    = Url.Absolute.Base("https://test.shipreq.com"),
    attackFrustrationDelay     = 1 hour,
    securityTokenLength        = 8,
    confirmationTokenLifespan  = 7 days,
    passwordResetTokenLifespan = 4 days,
    allowRegister              = Allow,
    taskmanSchema              = "test_taskman",
    initTaskmanOnBoot          = false,
    initTaskmanRetry           = RetryCriteria(2 hour, Some(666)))

  val user = User(UserId(1), Username("asds"), EmailAddr("x@x.com"), Set.empty)
  val ps = PasswordAndSalt(PasswordHash("wdsef34r"), Salt("32165498bdef"))

  final class Interpreters[F[_]](val run: F[_] => Any)(implicit val F: Monad[F]) {
    val self = this

    implicit object db extends DB.SecurityTokenReadOnly[F] with DB.ForSecurity[F] {
      var token = Option.empty[Instant]
      var getUserOk = true
      var projectOwner: Option[UserId] = Some(user.id)

      override def getUserRegistrationTokenIssueDate(t: SecurityToken)          = F point token
      override def getResetPasswordTokenIssueDate   (t: SecurityToken)          = F point token
      override def getUserAndPasswordByEmail        (e: EmailAddr)              = F.point(Option.when(getUserOk)((user, ps)))
      override def getUserAndPasswordByUsername     (u: Username)               = F.point(Option.when(getUserOk)((user, ps)))
      override def logLoginSuccess                  (i: UserId, ip: Option[IP]) = F.point(())
      override def getProjectOwner                  (id: ProjectId)             = F point projectOwner
    }

    implicit object security extends Security.Algebra[F] {
      var loginSuccess = true
      var loggedIn = Option.empty[User]

      override val db                                 = self.db
      val delay                                       = F.point(())
      override def protect[A](vulnerable: F[A])       = delay >> vulnerable
      override def hashPassword(p: PlainTextPassword) = F point ps
      override val isAuthenticated                    = F.point(loggedIn.isDefined)
      override val authenticatedUser                  = F.point(loggedIn)
      override val logout                             = F.point(loggedIn = None)

      override def attemptLogin(u: \/[Username, EmailAddr], p: PlainTextPassword) =
        F.point { loggedIn = Option.when(loginSuccess)(user); loggedIn }
    }

    implicit val svr: Server.Time[F] = new Server.Time[F] {
      override val now = F point Instant.now()
    }

    val dispatchLogic = new DispatchLogic[F]

    val dispatcher = dispatchLogic.main.withFallback(dispatchLogic.fallback)
  }

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  val DispatchRequests: List[DispatchLogic.Request] = {
    import DispatchLogic._
    import Method._
    val param: String => Option[String] = _ => None
    val token = SecurityToken("MnVC8cvPX9b1jiCpyxoYLk4RqQ8idHlV4lf7OHzIQctHLgw6C")
    val b = List.newBuilder[Request]
    b ++= Urls.PublicSpaRoute.static.whole.toList.map(r => Request(Get, r.url, param))
    b ++= Urls.PublicSpaRoute.needsToken.whole.toList.map(r => Request(Get, r.url(token), param))
    b ++= List(Urls.memberHome, Urls.publicHome).map(Request(Get, _, param))
    b.result()
  }

  implicit val monadCoeval: Monad[Coeval] = new Monad[Coeval] {
    override def point[A](a: => A): Coeval[A] = Coeval(a)
    override def bind[A, B](fa: Coeval[A])(f: (A) => Coeval[B]): Coeval[B] = fa flatMap f
    override def map[A, B](fa: Coeval[A])(f: (A) => B): Coeval[B] = fa map f
  }

  implicit val monadFuture: Monad[Future] = new Monad[Future] {
    import scala.concurrent.ExecutionContext.Implicits.global
    override def point[A](a: => A): Future[A] = Future(a)
    override def bind[A, B](fa: Future[A])(f: (A) => Future[B]): Future[B] = fa flatMap f
    override def map[A, B](fa: Future[A])(f: (A) => B): Future[B] = fa map f
  }

  val coeval     = new Interpreters[Coeval    ](_.apply())
  val io         = new Interpreters[IO        ](_.unsafePerformIO())
  val fn0        = new Interpreters[Function0 ](_.apply())
  val future     = new Interpreters[Future    ](Await.result(_, FiniteDuration(5, "min")))
  val name       = new Interpreters[Name      ](_.value)
  val trampoline = new Interpreters[Trampoline](_.run)
}
