ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.7"

lazy val root = (project in file("."))
  .settings(
    name := "akka_intro"
  )

val akkaVersion = "2.5.13"
ThisBuild / libraryDependencies ++= Seq(
//  ("com.typesafe.akka" %% "akka-actor-typed" % akkaVersion)
//        .cross(CrossVersion.for3Use2_13),
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
   "org.scalatest" %% "scalatest" % "3.0.5"
)