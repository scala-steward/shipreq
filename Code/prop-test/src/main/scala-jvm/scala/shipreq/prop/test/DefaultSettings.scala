package shipreq.prop.test

// ==================================
// ==========              ==========
// ========== JVM settings ==========
// ==========              ==========
// ==================================

object DefaultSettings {

  implicit val propSettings =
    Settings(
      executor   = ParallelExecutor(),
      sampleSize = SampleSize(1000),
      genSize    = GenSize(400))
}
