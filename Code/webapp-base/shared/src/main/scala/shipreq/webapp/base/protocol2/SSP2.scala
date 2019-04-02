package shipreq.webapp.base.protocol2test

import boopickle.{PickleState, Pickler, UnpickleState}
import boopickle.DefaultBasic._
import japgolly.univeq.UnivEq
import scalaz.\/
import shipreq.base.util.Url

object Protocol {

  trait Uni[F[_]] {
    type Type
    val codec: F[Type]
  }
  object Uni {
    def apply[F[_], T](c: F[T]): Aux[F, T] = new Uni[F] { type Type = T; val codec = c }
    type Aux[F[_], T] = Uni[F] { type Type = T }

//    def merge2[F[_], A, B](pA: Aux[F, A], pB: Aux[F, B]): Aux[F, A \/ B] =
//      new Uni[F] {
//        type Type = A \/ B
//        // mod codec
//      }
  }

  trait RequestResponse[F[_]] {
    val request: Uni[F]
    val response: Uni[F]
  }
}


//trait ProcProtocol {
//  type Request
//  type Response
//}
//
//object ProcProtocol {
//  trait Binary extends ProcProtocol {
//    val pickleRequest: Pickler[Request]
//    val pickleResponse: Pickler[Response]
//  }
//}

case class Proc[Id, P](id: Id, proc: P)

//class Procs[Id: UnivEq, P <: ProcProtocol](input: Traversable[Proc[Id, P]]) {
//  // TODO Assert ID uniqueness
//  val map: Map[Id, P] = ???
//}

//case class ServerSideProc[P](path: Url.Relative, proc: P)

object ServerSideProc {

  // NOTE: Paths are static; doesn't support GET /project/:id etc
  // case class AjaxBinary(path: Url.Relative, protocol: ProcProtocol.Binary)
//  type AjaxBinary = Proc[Url.Relative, ProcProtocol.Binary]

  type Binary = Proc[Url.Relative, Protocol.RequestResponse[Pickler]]
}

//case class WebSocketProc[K, P](key: K, proc: P)
trait WebSocketProtocol {

  val clientRequests: Map[String, Protocol.RequestResponse[Pickler]] = ???

//  val serverPush: Map[Int, Protocol.Uni[Pickler]]
  val serverPush: Protocol.Uni[Pickler]
}

trait AjaxUsage {
  type AsyncCallback[_]

  def clientRequest(p: ServerSideProc.Binary)
                   (i: p.proc.request.Type)
                   : AsyncCallback[p.proc.response.Type]

  type Fx[_]
  type SvrReq
  type SvrRes

  def serverResponse(p: ServerSideProc.Binary)
                    (f: p.proc.request.Type => Fx[p.proc.response.Type])
                     : SvrReq => Fx[SvrRes]
}

trait WebSocketUsage {
  type AsyncCallback[_]
  type WebSocket
  type ReqType

  // This would generate and register a unique request ID, which WS recv will invoke = AsyncCallback
  def clientRequest(p: Proc[ReqType, Protocol.RequestResponse[Pickler]])
                   (i: p.proc.request.Type)
                   (ws: WebSocket)
                   (implicit reqType: Pickler[ReqType])
                   : AsyncCallback[p.proc.response.Type]

  val serverPush: Protocol.Uni[Pickler]

  val onClientRecv: AsyncCallback[serverPush.Type]

//  type Fx[_]
//  type SvrReq
//  type SvrRes
//
//  def serverResponse(p: ServerSideProc.Binary)
//                    (f: p.proc.request.Type => Fx[p.proc.response.Type])
//                     : SvrReq => Fx[SvrRes]

}

object Sample {

  case class Request1(request1: String)
  case class Request2(request2: String)
  case class Response1(response1: String)
  case class Response2(response2: String)

  val uniRequest1  : Protocol.Uni.Aux[Pickler, Request1]  = ???
  val uniRequest2  : Protocol.Uni.Aux[Pickler, Request2]  = ???
  val uniResponse1 : Protocol.Uni.Aux[Pickler, Response1] = ???
  val uniResponse2 : Protocol.Uni.Aux[Pickler, Response2] = ???

  // --------------------------------------

  /** Eg. This could be ProjectSpaPush = Response1 | Response2 */
  sealed trait MultiWSPush
  object MultiWSPush {
    final case class P1(r: Response1) extends MultiWSPush
    final case class P2(r: Response2) extends MultiWSPush
  }

  val wsMultiPush = Protocol.Uni[Pickler, MultiWSPush](???)

  // --------------------------------------

  trait Request[F[_]] { self =>
    type ReqType
    val codecReq: F[ReqType]
    type ResType
    val codecRes: F[ResType]
    type AndRequest = Request.AndRequest[F] { val r: self.type }
    def andRequest(r: ReqType): AndRequest = ???
  }
  object Request {
    type Of[F[_], Req, Res] = Request[F] { type ReqType = Req; type ResType = Res }

    trait AndRequest[F[_]] {
      val r: Request[F]
      val req: r.ReqType
    }
  }

//  object StringToInt extends Request[Pickler] {
//    override type ReqType = String
//    override type ResType = Int
//    override val codecReq = ???
//    override val codecRes = ???
//  }
//  object LongToString extends Request[Pickler] {
//    override type ReqType = Long
//    override type ResType = String
//    override val codecReq = ???
//    override val codecRes = ???
//  }
//
//  def send(r: Request[Pickler])(i: r.ReqType): r.ResType = ???
//  send(StringToInt)("asd"): Int

//  case class KeyedRequest[F[_], Req, Res](key: Int, r: Request.Of[F, Req, Res])

//  sealed abstract class Sum(final val key: Int) {
//    val r: Request[Pickler]
//  }
//  object Sum {
//    case object String_Int extends Sum(1) {
//      override val r: StringToInt.type = StringToInt
//    }
//    case object Long_String extends Sum(2) {
//      override val r: LongToString.type = LongToString
//    }
//    val values  = Vector[Sum](String_Int, Long_String)
//    object X extends Request[Pickler] {
//      override type ReqType = String
//      override type ResType = Int
//      override val codecReq = ???
//      override val codecRes = ???
//    }
//  }

  sealed abstract class Sum(final val key: Int) extends Request[Pickler] { self =>
    object send extends Protocol.Uni[Pickler] {
      override type Type = (Int, self.ReqType)
      override val codec = ???
    }
    def build(in: ReqType)(ff: ResType => Unit): Built[Pickler] = new Built[Pickler] {
      object r extends Protocol.RequestResponse[Pickler] {
        override val request = self.send
        override val response = Protocol.Uni(self.codecRes)
      }
      override val i: r.request.Type = (key, in)
      override val f: r.response.Type => Unit = ff
    }
  }
  object Sum {
    object StringToInt extends Sum(1) {
      override type ReqType = String
      override type ResType = Int
      override val codecReq = ???
      override val codecRes = ???
    }
    object LongToString extends Sum(2) {
      override type ReqType = Long
      override type ResType = String
      override val codecReq = ???
      override val codecRes = ???
    }

//    val reader: Pickler
//    def send(r: Sum)(i: r.ReqType): r.ResType = ???
  }

  trait Built[F[_]] {
//    type I
//    type O
//    val key: Int
    val r: Protocol.RequestResponse[F]
    val i: r.request.Type
    val f: r.response.Type => Unit
  }

  def send(r: Built[Pickler]): Unit = ???
  send(Sum.StringToInt.build("asd")(i => println(i+1)))


  object StringToIntRR extends Protocol.RequestResponse[Pickler] { self =>
    override val request = Protocol.Uni[Pickler, String](???)
    override val response = Protocol.Uni[Pickler, Int](???)
    def build(in: request.Type)(ff: response.Type => Unit): Built[Pickler] = new Built[Pickler] {
      override val r: self.type = self
      override val i = in
      override val f = ff
    }
  }

//  trait ProtocolRR2[F[_]] {
//    type Req
//    type Res
//    def build(r: Req)(f: Res => Unit): Built[F]
//  }

  // TODO =========================================================================================================================================================
  // TODO THIS WORKS

  // 2 separate traits:
  // - {req; res; build(...)}
  // - {key; req; res; build(...)} or maybe {key?; subReq; SubRes; build(...)}
  // Or maybe one trait (without key), and an empty marker sub-trait to mark as conclusive?
  //   (i.e. prevent direct send() on PubSpa.Login ... but maybe that's ok cos I'm using to uni's instead of rr)

  // Tₙ & Tₙ.A
  // fold: foreach Tₙ:
  // - do stuff with t.A, return a t.B
  def serverSideHandler(in: Any): Any = {

    val f = PubSpa.Fold(login = i => s"${i.req + 10}")

    val tAndReq = PubSpa.AndReq.pickler.unpickle(???)
    val res = tAndReq.fold(f)
    tAndReq.p.res.codec.pickle(res)(???)
  }

  sealed trait PubSpa { self =>
    val req: Protocol.Uni[Pickler]
    val res: Protocol.Uni[Pickler]
    val key: Int
    final type AndReq = PubSpa.AndReq { val p: self.type }
    final def AndReq(r: req.Type): AndReq = ???

    final def build(in: req.Type)(ff: res.Type => Unit): Built[Pickler] = new Built[Pickler] {
      object r extends Protocol.RequestResponse[Pickler] {
        override val request  = Protocol.Uni(PubSpa.AndReq.pickler)
        override val response: res.type = res
      }
      override val i = self.AndReq(in)
      override val f = ff
    }
  }
  object PubSpa {
    object Login extends PubSpa {
      override val req = Protocol.Uni[Pickler, Int](???)
      override val res = Protocol.Uni[Pickler, String](???)
      override val key = 1
    }
    object Logout extends PubSpa {
      override val req = Protocol.Uni[Pickler, String](???)
      override val res = Protocol.Uni[Pickler, Boolean](???)
      override val key = 2
    }
    val byKey: Map[Int, PubSpa] = ???
    trait AndReq {
      val p: PubSpa
      val req: p.req.Type
      def fold(f: PubSpa.Fold): p.res.Type = ???
    }
    object AndReq {
      val pickler: Pickler[AndReq] = new Pickler[AndReq] {
        override def pickle(obj: AndReq)(implicit state: PickleState): Unit = {
          state.pickle(obj.p.key)
          state.pickle(obj.req)(obj.p.req.codec)
        }

        override def unpickle(implicit state: UnpickleState): AndReq = {
          val key = state.unpickle[Int]
          val p = byKey(key)
          val req = state.unpickle(p.req.codec)
          p.AndReq(req)
        }
      }
    }
    case class Fold(login: Login.AndReq => Login.res.Type)
  }
}