package shipreq.webapp.shared

object Interface {
  type Json = String

  sealed case class Defn[I, O] private (serialise: I => Json) {
    type Wired = Interface.Wired[this.type, I, O]
  }

  case class Wired[C <: Defn[I, O], I, O](fn: String, c: C)

  object Defn {
    object Square extends Defn[Int, String](_.toString)
    object Half extends Defn[Int, String](_.toString)
  }

  object Page {
    case class WIP(square: Defn.Square.Wired,
                   half: Defn.Half.Wired)
  }
}
