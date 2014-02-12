package appsh

import sbt._
import Keys._

object Appsh extends Plugin {
  object AppshKeys {
    val appshSource = SettingKey[File]("source for overriding defaults in project usually src/appsh")
    val appshZipDirectory = SettingKey[File]("directory that will be zipped. Usually target/appsh")
    val appshZipFile = SettingKey[File]("Zipfile. Usually target/appsh.zip")
    val appshPermissions = SettingKey[Map[String, Int]]("Map from path to unix permissions")
    val appshBuild = TaskKey[File]("appsh-build", "Creates a app.sh zip file.")
  }

  import AppshKeys._

  lazy val appshSettings: Seq[Setting[_]] = Seq(
    appshSource := baseDirectory.value / "src" / "appsh",
    appshZipDirectory := target.value / "appsh",
    appshZipFile := target.value / "appsh.zip",
    appshPermissions := Map(
      "bin/root/*" -> Integer.decode("0755"), 
      "hooks/*" -> Integer.decode("0755")
    ),
    appshBuild <<= (appshSource, appshZipDirectory, appshPermissions, appshZipFile, streams) map { (src, target, permissions, zip, stream) => 
      IO.delete(zip)
      
      val filesystem = Builder.buildFilesystem(target, src)
      val perms = Builder.preparePermissions(filesystem, permissions)

      Builder.makeZip(filesystem, perms, zip, stream.log)
    }
  )
}
