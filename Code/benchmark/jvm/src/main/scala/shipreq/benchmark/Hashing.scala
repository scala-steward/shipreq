package shipreq.benchmark

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import shipreq.webapp.base.data.Project
import shipreq.webapp.base.event.ApplyEvent
import shipreq.webapp.base.feature.hash.HashLogic
import shipreq.webapp.base.hash._

@Warmup(iterations = 20)
@Measurement(iterations = 20)
@Fork(2)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
class Hashing {

  val p = data.project_100

  @Benchmark def hashFull = HashSchemes.latest.hash(p)

  @Benchmark def changes = HashSchemes.latest.changes(Project.empty, p)

  val ves = data.EventStreamSample.ves
  @Benchmark def batch = ApplyEvent.eventBatcher.optimal(ves)

  val recs = HashSchemes.latest.changes(Project.empty, p)
  @Benchmark def validate = HashLogic.validate(recs, Project.empty, p)

}
