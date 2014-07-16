import complete.DefaultParsers._

name := "default_launcher"

appAssemblerSettings

appOutput in App := target.value / "appmgr" / "root"

appmgrSettings

appmgrOutputFile in Appmgr := target.value / "appmgr-build"

appmgrBuild <<= appmgrBuild.dependsOn(appAssemble)
