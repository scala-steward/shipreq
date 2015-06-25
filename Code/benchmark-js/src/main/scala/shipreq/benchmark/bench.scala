package shipreq.benchmark

import org.scalajs.dom.console
import scalajs.js
import js.annotation._
import js.JSApp

// TODO

@JSName("Benchmark")
class JsBenchmark(a: js.Any, b: js.Any = js.native, c: js.Any = js.native) extends js.Object {

  def run(options: js.Object = js.native): Unit = js.native
}

object ClientBench extends JSApp {
  val cp = new shipreq.webapp.client.ClientData(null)

  import BenchTmp._
  def demo(): String =
    chars.map(_.length).sum.toString

  def benchmark(f: () => Any) = {
    val jsFn: js.Function0[Any] = f

    def onCycle(updateEvery: Int): js.Function1[js.Any, Unit] = {
      var lastUpdateAt = 0
      var count = 0
      (ee: js.Any) => {
        val e = ee.asInstanceOf[js.Dynamic]

//        val cycles = e.target.stats.sample.length.asInstanceOf[Int]
//        val diff = cycles - lastUpdateAt
//        if (diff > updateEvery || diff < 0) {

        count += 1
        if (count == updateEvery) {
          lastUpdateAt += updateEvery
          count = 0
          console.log(e.target.toString())
        }
//        console.log(e.target.toString())

//        console.log(e)
//        console.log(e.target.count)
      }
    }

    // A Suite it seems
    val onComplete: js.ThisFunction0[js.Any, Unit] =
      (e: js.Any) => {
        console.log(e.asInstanceOf[js.Dynamic].toString())
        console.log("Done.")
      }

    val options = js.Dictionary.empty[Any]
    options.update("id", "blah")
    options.update("onCycle", onCycle(10))
    options.update("onComplete", onComplete)
    options.update("minSamples", 20)
    options.update("initCount", 20)
//    options.update("maxSamples", 200)
    options.update("maxTime", 60*2)
    options.update("async", true)

    val b = new JsBenchmark(f, options)
//    js.Dynamic.global.Benchmark
    println("Created. Running...")
    b.run()
  }

  def main(): Unit = {
    println("Yay!")

    benchmark(demo)

    println("Bye!")
  }
}