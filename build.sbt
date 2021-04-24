
name := "scala-zio-bookmarks"

version := "0.2"

scalaVersion := "2.13.3"

libraryDependencies += "org.jsoup" % "jsoup" % "1.13.1"
libraryDependencies += "dev.zio" %% "zio" % "1.0.7"

//import sbtassembly.AssemblyKeys._

assembly / mainClass := Some("in.diyd2.utils.ScalaBookmarksParser")
