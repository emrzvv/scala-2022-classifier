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