package shipreq.webapp.shared

object Interface {
  type Json = String

  sealed abstract class Defn {
    type I
    type O
    final type Wired = Interface.Wired[this.type]
  }

  sealed abstract class DefnImpl[II, OO] extends Defn {
    final override type I = II
    final override type O = OO
  }

  case class Wired[D <: Defn](n: String, d: D)

  object Defn {
    object Square extends DefnImpl[Int, String]
    object Half extends DefnImpl[Int, String]
    object Grrr extends DefnImpl[ExampleData, ExampleData]
  }

  object Page {
    case class WIP(square: Defn.Square.Wired,
                   half: Defn.Half.Wired,
                   grrr: Defn.Grrr.Wired)

  }
}

case class ExampleData(i: Int) {
  def yar = s"yar → $i"
}