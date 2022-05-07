ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "scala-2022-classifier"
  )

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"
libraryDependencies += "org.specs2" %% "specs2-core" % "4.14.1" % Test
libraryDependencies += "org.apache.lucene" % "lucene-analyzers-common" % "8.11.1"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.9"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.19"
libraryDependencies += "com.typesafe.akka" %% "akka-actor"           % "2.6.19"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.6.19"
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
libraryDependencies += "de.heikoseeberger" %% "akka-http-circe" % "1.39.2"
libraryDependencies += "io.circe"          %% "circe-core"           % "0.14.1"
libraryDependencies += "io.circe"          %% "circe-generic"        % "0.14.1"
libraryDependencies += "io.circe"          %% "circe-parser"         % "0.14.1"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.10"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

enablePlugins(SbtTwirl)