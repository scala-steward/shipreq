name := "Misc"

scalaVersion := "2.12.3"

scalacOptions := List(
	"-unchecked",
	"-deprecation",
	"-Ypartial-unification",
	"-Ypatmat-exhaust-depth", "off",
	"-Ywarn-inaccessible",
	"-feature", "-language:postfixOps", "-language:implicitConversions", "-language:higherKinds", "-language:existentials")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

triggeredMessage := Watched.clearWhenTriggered

fork in run := true

/*
val v = "0.3.2"
libraryDependencies += "com.google.cloud.trace" % "core"                   % v
//libraryDependencies += "com.google.cloud.trace" % "service"                % v
//libraryDependencies += "com.google.cloud.trace" % "logging-service"        % v
libraryDependencies += "com.google.cloud.trace" % "trace-grpc-api-service" % v
//libraryDependencies += "com.google.cloud.trace" % "servlet"                % v

libraryDependencies += "com.google.auth" % "google-auth-library-oauth2-http" % "0.7.1"
libraryDependencies += "io.grpc" % "grpc-netty" % "1.0.3"
libraryDependencies += "io.netty" % "netty-tcnative-boringssl-static" % "1.1.33.Fork23" % Runtime
*/

val v = "0.3.3"
  libraryDependencies += "com.github.japgolly.fork.google-cloud-trace" % "core"                   % v
//libraryDependencies += "com.github.japgolly.fork.google-cloud-trace" % "service"                % v
//libraryDependencies += "com.github.japgolly.fork.google-cloud-trace" % "logging-service"        % v
  libraryDependencies += "com.github.japgolly.fork.google-cloud-trace" % "trace-grpc-api-service" % v
//libraryDependencies += "com.github.japgolly.fork.google-cloud-trace" % "servlet"                % v

libraryDependencies += "com.google.auth" % "google-auth-library-oauth2-http" % "0.7.1"
libraryDependencies += "io.grpc" % "grpc-netty" % "1.5.0"
libraryDependencies += "io.netty" % "netty-tcnative-boringssl-static" % "2.0.5.Final" % Runtime
