name := "sbt-appmgr"

organization := "net.hamnaberg.sbt"

sbtPlugin := true

libraryDependencies += "net.hamnaberg" %% "scala-archiver" % "0.3.0-SNAPSHOT"

scalacOptions := Seq("-deprecation")

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + (version in ThisBuild).value)
}

scriptedBufferLog := false