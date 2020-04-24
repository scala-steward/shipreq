package shipreq.benchmark

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import shipreq.webapp.base.data._
import shipreq.webapp.sampledata.SampleData

/*
> jmh:run -wi 1 -i 3 -f 1 ReqCodesScanBM

[info] ReqCodesScanBM.scan     10000  avgt    3  0.623 ± 0.077  ms/op
 */

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class ReqCodesScanBM {

  @Param(Array("1000", "2000", "4000", "10000"))
//  @Param(Array("10000"))
  var events: String = _

  var trie: ReqCode.Trie = _

  @Setup
  def setup(): Unit = {
    trie = SampleData.byName(events).project.content.reqCodes.trie
  }

  @Benchmark def scan = ReqCodes.benchmarkScan(trie)
}
