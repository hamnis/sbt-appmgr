name := "sbt-appsh"

organization := "net.hamnaberg.sbt"

sbtPlugin := true

libraryDependencies += "net.hamnaberg" %% "scala-archiver" % "0.1.0"

scalacOptions := Seq("-deprecation")

ScriptedPlugin.scriptedSettings
