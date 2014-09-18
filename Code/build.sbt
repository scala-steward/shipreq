name := "ShipReq"

startYear := Some(2013)

initialize ~= { _ =>
  sys.props("scalac.patmat.analysisBudget") = "off"
}

// https://github.com/sbt/sbt/releases/tag/v0.13.6
updateOptions := updateOptions.value.withConsolidatedResolution(true)

// For jawn required by upickle
resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"
