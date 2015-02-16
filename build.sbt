name := "salesforce2hadoop"

organization := "com.datadudes"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.4"

resolvers += "Cloudera Repositories" at "https://repository.cloudera.com/artifactory/cloudera-repos"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging"  %% "scala-logging"    % "3.1.0",
  "ch.qos.logback"              % "logback-classic"   % "1.1.2",
  "org.slf4j"                   % "slf4j-api"         % "1.7.10",
  "org.scala-lang.modules"      %% "scala-xml"        % "1.0.2",
  "org.apache.avro"             % "avro"              % "1.7.5",
  "com.force.api"               % "force-wsc"         % "33.0.1" exclude("org.antlr", "ST4"),
  "com.force.api"               % "force-partner-api" % "33.0.1",
  "com.datadudes"               %% "wsdl2avro"        % "0.2-SNAPSHOT" changing(),
  "org.kitesdk"                 % "kite-hadoop-cdh5-dependencies" % "0.17.1" pomOnly()
    exclude("commons-beanutils", "commons-beanutils")
    exclude("org.slf4j", "slf4j-log4j12"),
  "org.kitesdk"                 % "kite-data-core"    % "0.17.1",
  "com.github.scopt"            %% "scopt"            % "3.3.0",
  "org.specs2"                  %% "specs2-junit"     % "2.4.15"    % "test"
)

mainClass in assembly := Some("com.datadudes.sf2hadoop.SFImportCLIRunner")

net.virtualvoid.sbt.graph.Plugin.graphSettings

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "maven", "org.slf4j", "slf4j-api", xs @ _*) => MergeStrategy.first
  case "org/apache/hadoop/yarn/factories/package-info.class"          => MergeStrategy.first
  case "org/apache/hadoop/yarn/factory/providers/package-info.class"  => MergeStrategy.first
  case "org/apache/hadoop/yarn/util/package-info.class"               => MergeStrategy.first
  case PathList("org", "apache", "commons", "collections", xs @ _*)   => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}