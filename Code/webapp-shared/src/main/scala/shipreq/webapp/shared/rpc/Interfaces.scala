package shipreq.webapp.shared.rpc

import Interface._
import upickle.key

object Interfaces {

  object Square extends DefT[Int, String]
  object Half extends DefT[Int, String]
  object Grrr extends DefT[ExampleData, ExampleData]

  case class WIP(@key("a") square: Square.Remote,
                 @key("b") half: Half.Remote,
                 @key("c") grrr: Grrr.Remote) extends Cluster

}

case class ExampleData(i: Int) {
  def yar = s"yar → $i"
}