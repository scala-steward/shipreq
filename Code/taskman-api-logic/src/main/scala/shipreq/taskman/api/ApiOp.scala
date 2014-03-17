package shipreq.taskman.api

/**
 * Represents an operation provided by the API.
 *
 * @tparam A The operation result.
 */
sealed trait ApiOp[A]

object ApiOp {

  case class SubmitMsg(m: Msg) extends ApiOp[Unit]

  case class SubmitMsgs(ms: Seq[Msg]) extends ApiOp[Unit]

  // -------------------------------------------------------------------------------------------------------------------

  object Effect {
    import scalaz.~>
    import scalaz.Free._
    import scalaz.Coyoneda.liftTF
    import scalaz.effect.IO
    import scalaz.std.function.function0Instance

    implicit def cmdLiftF[A](c: ApiOp[A]): FreeC[ApiOp, A] = liftFC(c)

    /** The IO monad ops will be converted into. */
    type IOM[A] = Function0[A]

    def io[A](a: => A): IOM[A] = () => a

    def compile[C[_], A](f: FreeC[C, A], t: C ~> IOM): IO[A] = {
      val g = f.mapSuspension(liftTF(t))
      IO{ g.run }
    }
  }
}
