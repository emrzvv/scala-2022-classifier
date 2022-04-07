ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "scala-2022-classifier"
  )

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"