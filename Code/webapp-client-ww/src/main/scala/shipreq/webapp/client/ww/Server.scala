package shipreq.webapp.client.ww

import shipreq.webapp.client.ww.api._
import Protocol._

trait Handler[Cmd[_]] {
  def apply[R](cmd: Cmd[R]): R
}

trait ResultEncoder[Cmd[_], W[_]] {
  def apply[R](cmd: Cmd[R]): W[R]
}

final class Server[Cmd[_], Encoded, R[_], W[_]](handler  : Handler[Cmd],
                                                resultEnc: ResultEncoder[Cmd, W],
                                                codec    : Codec[Encoded, R, W],
                                                interface: Interface[Encoded],
                                                onError  : OnError)
                                               (implicit readCmd: R[Cmd[_]]) {

  interface.listen(receiveRequest, onError)

  private def receiveRequest(m: Message[Encoded]): Unit = {
    trait R
    val cmd    = codec.decode[Cmd[_]](m.cmd).asInstanceOf[Cmd[R]]
    val result = handler(cmd)
    val re     = codec.encode(result)(resultEnc(cmd))
    val reply  = new Message(m.key, re)
    interface.post(reply)
  }
}

object Server extends Settings {
  import org.scalajs.dom.webworkers.DedicatedWorkerGlobalScope
  import codec._

  def apply[Cmd[_]](handler  : Handler[Cmd])
                   (resultEnc: ResultEncoder[Cmd, Writer])
                   (implicit cmdReader: Reader[Cmd[_]]): Server[Cmd, Encoded, Reader, Writer] =
    new Server(handler, resultEnc, codec, WebWorkerInterface, onError)

  object WebWorkerInterface extends Interface[Encoded] {
    private val worker = DedicatedWorkerGlobalScope.self

    override def listen(hnd: Message[Encoded] => Unit, onError: OnError): Unit = {
      worker.onmessage = Interface.onMessageFn(hnd)
    }

    override def post(msg: Message[Encoded]): Unit =
      worker.postMessage(msg, transferables(msg.cmd))
  }
}