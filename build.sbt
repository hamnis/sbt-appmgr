name := "sbt-appmgr"

organization := "net.hamnaberg.sbt"

sbtPlugin := true

libraryDependencies += "net.hamnaberg" %% "scala-archiver" % "0.2.0"

scalacOptions := Seq("-deprecation")

ScriptedPlugin.scriptedSettings
