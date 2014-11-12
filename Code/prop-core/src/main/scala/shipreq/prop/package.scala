package shipreq

import scala.annotation.elidable

package object prop {

  type FailureInfo = scalaz.Need[String]

  implicit class PropExtAny[A](val a: A) extends AnyVal {

    @elidable(elidable.ASSERTION)
    def assertSatisfies(p: Prop[A]): Unit = p.assert(a)
  }
}
