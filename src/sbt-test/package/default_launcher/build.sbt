import complete.DefaultParsers._

name := "default_launcher"

target in App := (target in Compile).value / "appmgr" / "root"

target in Appmgr := (target in Compile).value / "appmgr-build"

packageBin in Appmgr <<= (packageBin in Appmgr).dependsOn(packageBin in App)
