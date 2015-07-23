package shipreq.webapp.base.protocol

import boopickle._, BoopickleMacros._, BinCodecGeneric._, BinCodecEvents._
import shipreq.webapp.base.event.VerifiedEvents
import scalaz.{\/, Equal}

/**
 * A function that lives on the server to be invoked by the client.
 */
abstract class RemoteFn {
  type Input
  type Output
  type Failure
  final type Response = Failure \/ Output
  final type Instance = RemoteFn.InstanceFor[this.type]

  implicit val pickleInput   : Pickler[Input]
  implicit val pickleOutput  : Pickler[Output]
  implicit val pickleFailure : Pickler[Failure]
  implicit val pickleResponse: Pickler[Response]
}

/** Convenience constructor using [[GenericFailure]]. */
abstract class =>|=>[I, O](implicit I: Pickler[I], O: Pickler[O]) extends RemoteFn.AuxC[I, O, GenericFailure]

object RemoteFn {

  /** Convenience constructor returning [[VerifiedEvents]] and using [[GenericFailure]]. */
  abstract class ToVE[I](implicit I: Pickler[I]) extends (I =>|=> VerifiedEvents)

  /** Syntactic convenience that allows for single-line declaration. */
  abstract class AuxC[I, O, F](implicit I: Pickler[I], O: Pickler[O], F: Pickler[F]) extends RemoteFn {
    final override type Input   = I
    final override type Output  = O
    final override type Failure = F
    final override implicit val pickleInput    = I
    final override implicit val pickleOutput   = O
    final override implicit val pickleFailure  = F
    final override implicit val pickleResponse = BinCodecGeneric.pickleXor(F, O)
  }

  type Aux[I, O, F] = RemoteFn {type Input = I; type Output = O; type Failure = F}
  type AuxG[I, O] = Aux[I, O, GenericFailure]

  /**
   * A remote function that is online and ready for remote invocation.
   */
  trait Instance {
    /** The server-side Lift function key. */
    val key: String
    val fn: RemoteFn
  }

  type InstanceFor[F <: RemoteFn] = Instance { val fn: F }

  def Instance(k: String, f: RemoteFn): InstanceFor[f.type] =
    new Instance {
      override val key = k
      override val fn: f.type = f
    }

  implicit def equalFn[F <: RemoteFn]: Equal[F] =
    Equal.equalRef

  implicit def equalInstance: Equal[Instance] =
    Equal.equal((a, b) => (a.key == b.key) && equalFn.equal(a.fn, b.fn))
}

/** The default representation of [[RemoteFn]] failure. */
case class GenericFailure(msg: String)

object GenericFailure {
  implicit val pickleGenericFailure: Pickler[GenericFailure] = pickleCaseClass
}
