package shipreq.webapp

import japgolly.univeq.UnivEq

package object gen {

  final case class Html(value: String) extends AnyVal {
    @inline def map(f: String => String): Html =
      Html(f(value))
  }

  object Html {
    implicit def univEq: UnivEq[Html] = UnivEq.derive
  }

  case class MainAndTest[A](main: A, tests: Vector[A]) {
    def map[B](f: A => B): MainAndTest[B] =
      MainAndTest(f(main), tests map f)
  }

  val ExpectedTemplateCount = 1
}
