// Project info

name := "play-mongo-jackson-mapper"

organization := "net.vz.mongodb.jackson"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.9.1"

// Dependencies

libraryDependencies ++= Seq(
    "net.vz.mongodb.jackson" % "mongo-jackson-mapper" % "1.4.0",
    "com.fasterxml" % "jackson-module-scala" % "1.9.3",
    "play" %% "play" % "2.0"
)

// Test dependencies

libraryDependencies ++= Seq(
    "org.specs2" %% "specs2" % "1.8.2" % "test",
    "play" %% "play-test" % "2.0" % "test"
)