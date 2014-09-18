package shipreq.webapp.shared

/*
object Interface {
  type Json = String

  sealed trait Types[DD <: Defn[II, OO], II, OO] {
    type I = II
    type O = OO
    type D = DD
  }

  sealed case class Defn[I, O] private (serialise: I => Json) {
    type Wired = Interface.Wired[this.type, I, O]
    type T = Types[this.type, I, O]
  }

  case class Wired[D <: Defn[I, O], I, O](n: String, d: D)

  object Defn {
    object Square extends Defn[Int, String](_.toString)
    object Half extends Defn[Int, String](_.toString)
  }

  object Page {
    case class WIP(square: Defn.Square.Wired,
                   half: Defn.Half.Wired)
  }
}
*/

object Interface {
  type Json = String

  sealed abstract class Defn {
    type I
    type O
    final type Wired = Interface.Wired[this.type]
    def serialise: I => Json
  }

  sealed abstract class DefnImpl[II, OO](override val serialise: II => Json) extends Defn {
    final override type I = II
    final override type O = OO
  }

  case class Wired[D <: Defn](n: String, d: D)

  object Defn {
    object Square extends DefnImpl[Int, String](_.toString)
    object Half extends DefnImpl[Int, String](_.toString)
  }

  object Page {
    case class WIP(square: Defn.Square.Wired,
                   half: Defn.Half.Wired)
  }
}
