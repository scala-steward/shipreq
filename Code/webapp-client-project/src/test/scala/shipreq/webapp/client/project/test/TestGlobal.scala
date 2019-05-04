package shipreq.webapp.client.project.test

import boopickle.Pickler
import japgolly.scalajs.react.{Callback, CallbackTo}
import scalaz.\/-
import shipreq.base.util.JsExt._
import shipreq.base.util.Retries
import shipreq.webapp.base.data.Project
import shipreq.webapp.base.event._
import shipreq.webapp.base.test.WebappTestUtil._
import shipreq.webapp.base.protocol._
import shipreq.webapp.base.protocol.ProjectSpaProtocols.WsReqRes
import shipreq.webapp.base.protocol.WebSocket.ReadyState
import shipreq.webapp.client.project.app.state.{Global, ProjectState}

final class TestGlobal(initialProjectState: ProjectState) extends Global((_, _) => Callback.empty, _ => Callback.empty) {

  lazy val protocol = ProjectSpaProtocols.WebSocket(initialProjectState.projectMetaData.id)

  lazy val svr = WebSocketServerHelper(protocol)

  case class Req(msg: FakeWebSocket.Message) {
    lazy val (reqId, req) = BinaryJs.decodeUnsafe(msg.binaryData, svr.protocolCS)

    def respondWith(r: Protocol.AndValue[Pickler]): Unit = {
      val res = \/-((reqId, r))
      val bd = BinaryJs.encode(svr.protocolSC)(res)
      val msg = FakeWebSocket.Message.ArrayBuffer(bd.toArrayBuffer)
      ws().recv(msg)
    }
  }

  private var _reqs = Vector.empty[Req]

  private var _fakeWS = Vector.empty[FakeWebSocket]

  override val wsClient: WebSocketClient[WsReqRes] = {
    val newWS = CallbackTo {
      val f = new FakeWebSocket("whatever", ReadyState.Open)
      f.onSend.set(m => _reqs :+= Req(m))
      _fakeWS :+= f
      f
    }
    WebSocketClient(newWS, protocol, Retries.none)
      .build(onPush, _ => onWebSocketReadyStateChange)
  }

  val nextEventOrd: CallbackTo[EventOrd] =
    CallbackTo {
      val s = unsafeState.asInstanceOf[Global.State.Active].projectState
      assert(s.futureEvents.isEmpty)
      s.projectAndOrd.nextOrd
    }

  def ws() = _fakeWS.last
  def reqs() = _reqs

  def assertReqsSent(count: Int)(implicit s: sourcecode.Enclosing, l: sourcecode.Line): Unit =
    assertEq(s"[${s.value}: ${l.value}] Requests sent", reqs().size, count)

  def respondToLast(p: WsReqRes)(o: p.ResponseType): Unit =
    reqs().last.respondWith(p.protocolRes.andValue(o))

  def verifyEventsCB(es: Event*): CallbackTo[VerifiedEvent.Seq] = {
    val eventList = es.toList // avoid Scala bug
    pxProject.toCallback.flatMap(p =>
      CallbackTo.liftTraverse((e: Event) => nextEventOrd.map(verifyEvent(p, e, _))).std[List]
        .map(VerifiedEvent.Seq.empty ++ _ (eventList)))
  }

  def applyTestEventsCB(es: Event*): Callback =
    verifyEventsCB(es: _*).flatMap(addEvents)

  unsafeSetState(Global.State.Active(initialProjectState))
  wsClient.connect.runNow()
}

object TestGlobal {

  def apply(p: Project): TestGlobal = {
    val md  = looseProjectMetaData(p, totalEventCount = 200)
    val pao = ProjectAndOrd(p, Some(EventOrd.Latest(200)))
    val ps  = ProjectState.init(pao, md)
    new TestGlobal(ps)
  }

}
