package shipreq.webapp.client.test

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra._

class TestVar[A](init: A) {

  var value = init

  val reusableSet: A ~=> Callback =
    ReusableFn(a => Callback(value = a))

  def reusableVar(implicit r: Reusability[A]): ReusableVar[A] =
    ReusableVar(value)(reusableSet)(r)

  def externalVar: ExternalVar[A] =
    ExternalVar(value)(reusableSet)
}


object TestVar {
  def apply[A](a: A): TestVar[A] = new TestVar(a)
  def init [A](a: A): TestVar[A] = new TestVar(a)
}