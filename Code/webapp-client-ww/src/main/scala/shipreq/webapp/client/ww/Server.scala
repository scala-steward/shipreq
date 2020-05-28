package shipreq.webapp.client.ww

import japgolly.scalajs.react.{Callback, CallbackTo}
import org.scalajs.dom.webworkers.DedicatedWorkerGlobalScope
import shipreq.webapp.client.ww.api.Protocol._

object Server {

  def startDefault[Cmd[_]](service: Service[Cmd],
                           resultEnc: ResultEncoder[Cmd, Codec.default.Writer])
                          (implicit cmdReader: Codec.default.Reader[Cmd[_]]): Callback = {
    import Codec.{default => codec}
    start(codec, service)(interface(codec), resultEnc, OnError.logToConsole)
  }

  def start[Cmd[_]](codec    : Codec,
                    service  : Service[Cmd])
                   (interface: Interface[codec.Encoded],
                    resultEnc: ResultEncoder[Cmd, codec.Writer],
                    onError  : OnError)
                   (implicit cmdReader: codec.Reader[Cmd[_]]): Callback = {

    import codec.Encoded

    def respondForSome[A](id: Int, cmd: Cmd[A]): Callback =
      CallbackTo {
        val a = service(cmd)
        val e = codec.encode(a)(resultEnc(cmd))
        new Message(id, e)
      }.flatMap(interface.post(_))

    def respond(m: Message[Encoded]): Callback = {
      val cmd = codec.decode[Cmd[_]](m.cmd)
      respondForSome(m.id, cmd)
    }

    interface.listen(respond, onError)
  }

  // ===================================================================================================================

  trait Service[Cmd[_]] {
    def apply[R](cmd: Cmd[R]): R
  }

  trait ResultEncoder[Cmd[_], W[_]] {
    def apply[A](cmd: Cmd[A]): W[A]
  }

  // ===================================================================================================================

  def interface(codec: Codec): Interface[codec.Encoded] =
    new Interface[codec.Encoded] {
      import codec.Encoded

      private[this] val worker = DedicatedWorkerGlobalScope.self

      override def listen(hnd: Message[Encoded] => Callback, onError: OnError): Callback =
        Callback {
          worker.onmessage = Interface.onMessageFn(hnd)
          worker.onError = Interface.onErrorFn(onError.handle)
        }

      override def post(msg: Message[Encoded]): Callback =
        Callback(worker.postMessage(msg, codec.transferables(msg.cmd)))
    }
}