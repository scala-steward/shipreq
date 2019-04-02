package shipreq.webapp.base.protocol2

import scalaz.\/
import shipreq.base.util.Url

object Protocol {

  trait Channel[F[_]] {
    type Type
    val codec: F[Type]
  }

  object Channel {

    type Of[F[_], A] = Channel[F] { type Type = A }

    def apply[F[_], A](c: F[A]): Of[F, A] =
      new Channel[F] {
        override type Type = A
        override val codec = c
      }
  }

  // ===================================================================================================================

  trait RequestResponse[F[_]] {
    val request : Channel[F]
    val response: Channel[F]
  }

  object RequestResponse {

    type Of[F[_], Req, Res] = RequestResponse[F] {
      val request : Channel.Of[F, Req]
      val response: Channel.Of[F, Res]
    }

    def apply[F[_], Req, Res](req: Channel.Of[F, Req], res: Channel.Of[F, Res]): Of[F, Req, Res] =
      new RequestResponse[F] {
        override val request  = req
        override val response = res
      }
  }

  // ===================================================================================================================

  final case class Ajax[F[_], Req, Res](url        : Url.Relative,
                                        protocol   : RequestResponse.Of[F, Req, Res])

  // ===================================================================================================================

//  final case class WebSocket[F[_], Res, Req, Push](fromClient: Protocol.RequestResponse.Of[F, Req, Res],
//                                                   fromServer: Protocol.Channel.Of[F, Push])

  final case class WebSocket[F[_], CS, SC](clientToServer: Protocol.Channel.Of[F, CS],
                                           serverToClient: Protocol.Channel.Of[F, SC])

  object WebSocket {

    type ClientReqAndServerPush[F[_], ReqId, Req, Res, Push] =
      WebSocket[F, (ReqId, Req), Push \/ (ReqId, Res)]

  }
}
