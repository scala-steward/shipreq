package shipreq.webapp.shared.rpc

object Interface {

  /**
   * Definition of a remote call.
   */
  abstract class Def {
    type I
    type O
    final type Remote = Interface.Remote[this.type]
  }

  /**
   * Syntactic convenience that allows for single-line declaration.
   */
  abstract class DefT[I_, O_] extends Def {
    final override type I = I_
    final override type O = O_
  }

  /**
   * Descriptor of a remotely available RPC.
   * @param n The server-side Lift function key.
   */
  case class Remote[D <: Def](n: String, d: D)

  // TODO Customise serialisation for Remote
  // {"square":{"n":"F751737835735KSD2LY","d":{}} should just be "F751737835735KSD2LY"

  /** Denotes a set of interfaces. */
  trait Cluster
}