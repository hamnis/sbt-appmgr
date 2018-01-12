name := "default_launcher"

enablePlugins(UniversalPlugin)

stagingDirectory in Universal := (target in Compile).value / "appmgr" / "root"

target in Appmgr := (target in Compile).value / "appmgr-build"

packageBin in Appmgr := {
    val stageResult = (stage in Universal).value
    (packageBin in Appmgr).value
}
