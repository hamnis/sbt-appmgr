val pluginVersion = scala.util.Properties.propOrNone("plugin.version").getOrElse(
throw new RuntimeException("""
  |The system property 'plugin.version' is not defined.
  |Specify this property using the scriptedLaunchOpts -D.
""".stripMargin))

addSbtPlugin("net.hamnaberg.sbt" % "sbt-appmgr" % pluginVersion)

//addSbtPlugin("net.hamnaberg.sbt" % "sbt-appassembler" % "0.6.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")