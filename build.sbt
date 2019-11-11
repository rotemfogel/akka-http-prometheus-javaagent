name := "akka-http-prometheus-javaagent"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies ++= {
  val akkaVersion     = "2.5.26"
  val akkaHttpVersion = "10.1.0"
  val json4sVersion   = "3.5.4"

  Seq(
    "org.slf4j"             % "slf4j-api"           % "1.7.26",
    "ch.qos.logback"        % "logback-classic"     % "1.2.3",
    "com.typesafe.akka"    %% "akka-slf4j"          % akkaVersion,
    "com.typesafe.akka"    %% "akka-stream"         % akkaVersion,
    "com.typesafe.akka"    %% "akka-http"           % akkaHttpVersion,
    "io.dropwizard.metrics" % "metrics-core"        % "4.1.1",
    "org.json4s"           %% "json4s-native"       % json4sVersion,
    "org.json4s"           %% "json4s-ext"          % json4sVersion,

    // test dependencies
    "org.scalatest"        %% "scalatest"           % "3.0.8"         % Test,
    "com.typesafe.akka"    %% "akka-stream-testkit" % akkaVersion     % Test,
    "com.typesafe.akka"    %% "akka-http-testkit"   % akkaHttpVersion % Test
  )
}

assemblyMergeStrategy in assembly := {
  case "log4j.properties"                                         => MergeStrategy.first
  case PathList("test", "scala", "resources", "application.conf") => MergeStrategy.discard
  case x if x.endsWith("io.netty.versions.properties")            => MergeStrategy.first
  case x if x.endsWith("module-info.class")                       => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*)                            => MergeStrategy.first
  case s if s.endsWith("public-suffix-list.txt")                  => MergeStrategy.discard
  case x if x.endsWith("mozilla/public-suffix-list.txt")          => MergeStrategy.first
  case "application.conf"                                         => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}