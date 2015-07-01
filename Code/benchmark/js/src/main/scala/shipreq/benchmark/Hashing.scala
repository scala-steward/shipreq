package shipreq.benchmark

import shipreq.webapp.base.hash.Hash.HashableValueOps
import shipreq.webapp.base.hash.{Hash, HashScheme}

object Hashing {
  implicit val projectHash = HashScheme.default.hashProject
  val p100  = data.project_100
  val p1000 = data.project_1000

  val prefix = "Hashing."

  var bms = Vector.empty[Benchmark]

  def addBenchmark[A: Hash](name: String, start: => A): Unit =
    bms :+= Benchmark.init(prefix + name, start)(_.hash)

  addBenchmark("hash_100",  p100)
  addBenchmark("hash_1000", p1000)

  def run(): Unit = {
    val o = Benchmark.defaultOptions()
    // o.maxTime = 30
    Benchmark.run(bms: _*)(o)
  }
}