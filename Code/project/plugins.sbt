addSbtPlugin("com.earldouglas"    % "xsbt-web-plugin"      % "4.0.1")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"          % "0.3.3")
addSbtPlugin("com.typesafe.sbt"   % "sbt-git"              % "0.9.3")
addSbtPlugin("com.typesafe.sbt"   % "sbt-native-packager"  % "1.3.2")
addSbtPlugin("net.virtual-void"   % "sbt-dependency-graph" % "0.9.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"          % "0.6.19")
//addSbtPlugin("org.scoverage"      % "sbt-scoverage"        % "1.5.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"              % "0.3.2")
addSbtPlugin("se.marcuslonnberg"  % "sbt-docker"           % "1.5.0")

// Facilitates running Scala.JS tests in real browsers
libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "0.1.3"

// https://github.com/JetBrains/sbt-ide-settings
resolvers += Resolver.url("jetbrains-bintray", url("http://dl.bintray.com/jetbrains/sbt-plugins/"))(Resolver.ivyStylePatterns)
addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "0.1.1")
