package shipreq.prop.test

import scalaz.EphemeralStream
import scalaz.effect.IO
import shipreq.prop.Prop
import PTest._

trait Executor {
  def run[A](p: Prop[A], g: SampleSize => IO[EphemeralStream[A]], S: Settings): RunState[A]
}

object SingleThreadedExecutor extends Executor {
  override def run[A](p: Prop[A], g: SampleSize => IO[EphemeralStream[A]], S: Settings): RunState[A] = {
    val data = g(S.sampleSize).unsafePerformIO()
    var i = 0
    testN(p, data, () => {i+=1; i}, S)
  }
}