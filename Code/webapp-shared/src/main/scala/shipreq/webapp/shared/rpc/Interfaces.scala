package shipreq.webapp.shared.rpc

import Interface._

object Interfaces {

  object Square extends DefT[Int, String]
  object Half extends DefT[Int, String]
  object Grrr extends DefT[ExampleData, ExampleData]

  case class WIP(square: Square.Remote,
                 half: Half.Remote,
                 grrr: Grrr.Remote) extends Cluster

}

case class ExampleData(i: Int) {
  def yar = s"yar → $i"
}