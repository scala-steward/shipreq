package shipreq.webapp.client.ww

import scalajs.js.annotation._
import shipreq.webapp.client.ww.api._
import Server.codec.Writer

@JSExport("Main")
object Main {

  @JSExport
  def main(): Unit = {
    Server(Handler)(ResultEncoder)
  }

  object ResultEncoder extends ResultEncoder[Cmd, Writer] {
    override def apply[R](cmd: Cmd[R]): Writer[R] =
      cmd.resultPickler
  }

  object Handler extends Handler[Cmd] {
    override def apply[R](cmd: Cmd[R]): R =
      cmd match {
        case Cmd.GraphUseCaseSteps(_, _) => SVG("qwe")
      }
  }
}


