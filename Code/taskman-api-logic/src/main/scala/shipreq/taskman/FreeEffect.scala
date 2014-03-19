package shipreq.taskman

import scalaz.~>
import scalaz.Free
import scalaz.Free.FreeC
import scalaz.Coyoneda.liftTF
import scalaz.effect.IO
import scalaz.std.function.function0Instance

object FreeEffect {

  implicit class OpExt[O[_], A](val a: O[A]) extends AnyVal {
    def liftFC: FreeC[O, A] = Free.liftFC(a)
    def >>[B](b: FreeC[O, B]): FreeC[O, B] = a.liftFC.flatMap(_ => b)
    def >>[B](b: O[B]): FreeC[O, B] = a >> b.liftFC
  }

  implicit class FreeCExt[O[_], A](val a: FreeC[O, A]) extends AnyVal {
    def >>[B](b: O[B]): FreeC[O, B] = a.flatMap(_ => b.liftFC)
  }

  implicit def opLiftFC[O[_], A](o: O[A]): FreeC[O, A] = o.liftFC

  /** The IO monad ops will be converted into. */
  type IOM[A] = Function0[A]

  def iom[A](a: => A): IOM[A] = () => a

  def compile[C[_], A](f: FreeC[C, A], t: C ~> IOM): IO[A] = {
    val g = f.mapSuspension(liftTF(t))
    IO{ g.run }
  }
}
