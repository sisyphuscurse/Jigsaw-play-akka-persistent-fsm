name := """JigsawOnPlay"""
organization := "com.yiguanjinrong"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "maven(org.scalatestplus.play, scalatestplus-play_2.12, stable)" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.yiguanjinrong.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.yiguanjinrong.binders._"
