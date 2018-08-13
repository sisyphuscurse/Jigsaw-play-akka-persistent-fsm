name := """JigsawOnPlay"""
organization := "com.yiguan"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

libraryDependencies += guice

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test

libraryDependencies += "com.github.scullxbones" %% "akka-persistence-mongo-casbah" % "2.0.3"

libraryDependencies += "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "2.0.3"

libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.12.5"

libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.12.5-play26"

libraryDependencies += "org.mongodb" % "casbah_2.12" % "3.1.1"

libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.5.3"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.3" % Test

libraryDependencies += "org.mongodb" % "mongodb-driver" % "3.4.2"

libraryDependencies += ws

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % Test

libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1" % Test

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test

libraryDependencies += "de.leanovate.play-mockws" %% "play-mockws" % "2.6.0" % Test

mappings in Universal ++=
  (baseDirectory.value / "scripts" * "*" get) map
    (x => x -> ("" + x.getName))
