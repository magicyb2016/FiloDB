val mySettings = Seq(organization := "org.velvia",
                     scalaVersion := "2.10.4",
                     parallelExecution in Test := false,
                     fork in Test := true,
                     resolvers ++= extraRepos,
                     ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }) ++ universalSettings

lazy val core = (project in file("core"))
                  .settings(mySettings:_*)
                  .settings(name := "filodb-core")
                  .settings(scalacOptions += "-language:postfixOps")
                  .settings(libraryDependencies ++= coreDeps)

lazy val coordinator = (project in file("coordinator"))
                         .settings(mySettings:_*)
                         .settings(name := "filodb-coordinator")
                         .settings(libraryDependencies ++= coordDeps)
                         .dependsOn(core % "compile->compile; test->test")

lazy val cassandra = (project in file("cassandra"))
                       .settings(mySettings:_*)
                       .settings(name := "filodb-cassandra")
                       .settings(libraryDependencies ++= cassDeps)
                       .dependsOn(core % "compile->compile; test->test")

lazy val cli = (project in file("cli"))
                 .settings(mySettings:_*)
                 .settings(name := "filodb-cli")
                 .settings(libraryDependencies ++= cliDeps)
                 .settings(cliAssemblySettings:_*)
                 .dependsOn(core, coordinator, cassandra)

lazy val spark = (project in file("spark"))
                   .settings(name := "filodb-spark")
                   .configs( IntegrationTest )
                   .settings(itSettings : _*)
                   .settings(fork in IntegrationTest := true)
                   .settings(mySettings:_*)
                   .settings(libraryDependencies ++= sparkDeps)
                   .settings(assemblySettings:_*)
                   .settings(assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false))
                   .dependsOn(core % "compile->compile; test->test; it->test",
                              coordinator,
                              cassandra % "compile->compile; test->test; it->test")

lazy val jmh = (project in file("jmh"))
                 .settings(mySettings:_*)
                 .settings(name := "filodb-jmh")
                 .settings(libraryDependencies ++= jmhDeps)
                 .enablePlugins(JmhPlugin)
                 .dependsOn(core % "compile->compile; compile->test", spark)

lazy val stress = (project in file("stress"))
                    .settings(mySettings:_*)
                    .settings(libraryDependencies ++= stressDeps)
                    .settings(assemblySettings:_*)
                    .dependsOn(spark)

val cassDriverVersion = "3.0.2"
val akkaVersion    = "2.3.15"
val sparkVersion   = "1.5.1"

lazy val extraRepos = Seq(
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
  "twitter-repo" at "http://maven.twttr.com",
  "Websudos releases" at "https://dl.bintray.com/websudos/oss-releases/",
  "Pellucid Bintray" at "http://dl.bintray.com/pellucid/maven",
  "Velvia Bintray" at "https://dl.bintray.com/velvia/maven",
  "spray repo" at "http://repo.spray.io"
)

val excludeShapeless = ExclusionRule(organization = "com.chuusai")
// Zookeeper pulls in slf4j-log4j12 which we DON'T want
val excludeZK = ExclusionRule(organization = "org.apache.zookeeper")
// This one is brought by Spark by default
val excludeSlf4jLog4j = ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12")
val excludeJersey = ExclusionRule(organization = "com.sun.jersey")

lazy val coreDeps = Seq(
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.slf4j"             % "slf4j-api"         % "1.7.10",
  "com.beachape"         %% "enumeratum"        % "1.2.1",
  "org.velvia.filo"      %% "filo-scala"        % "0.2.4",
  "joda-time"             % "joda-time"         % "2.2",
  "org.joda"              % "joda-convert"      % "1.2",
  "io.spray"             %% "spray-caching"     % "1.3.2",
  "net.ceedubs"          %% "ficus"             % "1.0.1",
  "org.scodec"           %% "scodec-bits"       % "1.0.10",
  "org.scalactic"        %% "scalactic"         % "2.2.6",
  "com.markatta"         %% "futiles"           % "1.1.3",
  "com.nativelibs4java"  %% "scalaxy-loops"     % "0.3.3" % "provided",
  "ch.qos.logback"        % "logback-classic"   % "1.0.7" % "test",  // to get good test logs
  "org.scalatest"        %% "scalatest"         % "2.2.4" % "test"
)

lazy val cassDeps = Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % cassDriverVersion,
  "ch.qos.logback"        % "logback-classic"   % "1.0.7" % "test"
)

lazy val coordDeps = Seq(
  "com.typesafe.akka"    %% "akka-slf4j"        % akkaVersion,
  "com.typesafe.akka"    %% "akka-cluster"      % akkaVersion,
  "com.opencsv"           % "opencsv"           % "3.3",
  "com.typesafe.akka"    %% "akka-testkit"      % akkaVersion % "test",
  "ch.qos.logback"        % "logback-classic"   % "1.0.7" % "test",  // to get good test logs
  "org.scalatest"        %% "scalatest"         % "2.2.4" % "test"
)

lazy val cliDeps = Seq(
  "ch.qos.logback"        % "logback-classic"   % "1.0.7",
  "com.quantifind"       %% "sumac"             % "0.3.0"
)

lazy val sparkDeps = Seq(
  // We don't want LOG4J.  We want Logback!  The excludeZK is to help with a conflict re Coursier plugin.
  "org.apache.spark"     %% "spark-hive"         % sparkVersion % "provided" excludeAll(excludeSlf4jLog4j, excludeZK),
  "org.apache.spark"     %% "spark-hive-thriftserver" % sparkVersion % "provided" excludeAll(excludeSlf4jLog4j, excludeZK),
  "org.apache.spark"     %% "spark-streaming"   % sparkVersion % "provided",
  "org.scalatest"        %% "scalatest"         % "2.2.4" % "it"
)

lazy val jmhDeps = Seq(
  "com.nativelibs4java"  %% "scalaxy-loops"     % "0.3.3" % "provided",
  "org.apache.spark"     %% "spark-sql"         % sparkVersion excludeAll(
                                                    excludeSlf4jLog4j, excludeZK, excludeJersey)
)

lazy val stressDeps = Seq(
  "com.databricks"       %% "spark-csv"         % "1.3.0",
  "org.apache.spark"     %% "spark-sql"         % sparkVersion % "provided" excludeAll(excludeZK),
  "org.apache.spark"     %% "spark-streaming"   % sparkVersion % "provided" excludeAll(excludeZK)
)

//////////////////////////
///

lazy val coreSettings = Seq(
  scalacOptions ++= Seq("-Xlint", "-deprecation", "-Xfatal-warnings", "-feature")
)

lazy val testSettings = Seq(
    parallelExecution in Test := false,
    // Needed to avoid cryptic EOFException crashes in forked tests
    // in Travis with `sudo: false`.
    // See https://github.com/sbt/sbt/issues/653
    // and https://github.com/travis-ci/travis-ci/issues/3775
    javaOptions += "-Xmx1250M",
    concurrentRestrictions in Global := Seq(
      // Tags.limit(Tags.CPU, java.lang.Runtime.getRuntime().availableProcessors()),
      Tags.limit(Tags.CPU, 1),
      // limit to 1 concurrent test task, even across sub-projects
      Tags.limit(Tags.Test, 1),
      // Note: some components of tests seem to have the "Untagged" tag rather than "Test" tag.
      // So, we limit the sum of "Test", "Untagged" tags to 1 concurrent
      Tags.limitSum(1, Tags.Test, Tags.Untagged))
)

lazy val itSettings = Defaults.itSettings ++ Seq(
  fork in IntegrationTest := true
)

lazy val universalSettings = coreSettings ++ styleSettings ++ testSettings

// Create a default Scala style task to run with tests
lazy val testScalastyle = taskKey[Unit]("testScalastyle")

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val styleSettings = Seq(
  testScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Test).toTask("").value,
  // (scalastyleConfig in Test) := "scalastyle-test-config.xml",
  // This is disabled for now, cannot get ScalaStyle to recognize the file above for some reason :/
  // (test in Test) <<= (test in Test) dependsOn testScalastyle,
  scalastyleFailOnError := true,
  compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value,
  // Is running this on compile too much?
  (compile in Test) <<= (compile in Test) dependsOn compileScalastyle
)

lazy val shellScript = """#!/bin/bash
while [ "${1:0:2}" = "-D" ]
do
  allprops="$allprops $1"
  shift
done
if [ ! -z "$FILO_CONFIG_FILE" ]; then
  config="-Dconfig.file=$FILO_CONFIG_FILE"
fi
exec java -Xmx4g -Xms4g $config $allprops -jar "$0" "$@"
""".split("\n")

// Builds cli as a standalone executable to make it easier to launch commands
lazy val cliAssemblySettings = assemblySettings ++ Seq(
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(
                                  prependShellScript = Some(shellScript)),
  assemblyJarName in assembly := s"filo-cli-${version.value}",
  logLevel in assembly := Level.Error
)

lazy val assemblySettings = Seq(
  assemblyMergeStrategy in assembly := {
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.properties") => MergeStrategy.discard
    case PathList(ps @ _*) if ps.last endsWith ".txt.1" => MergeStrategy.first
    case "reference.conf"    => MergeStrategy.concat
    case "application.conf"  => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  assemblyShadeRules in assembly := Seq(
    ShadeRule.rename("com.datastax.driver.**" -> "filodb.datastax.driver.@1").inAll
  ),
  test in assembly := {} //noisy for end-user since the jar is not available and user needs to build the project locally
)
