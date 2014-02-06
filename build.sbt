name := "sbt-appsh"

organization := "net.hamnaberg.sbt"

sbtPlugin := true

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.7"

scalacOptions := Seq("-deprecation")

ScriptedPlugin.scriptedSettings
