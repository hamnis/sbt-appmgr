package appsh

import sbt._
import Keys._
import archiver.{FilePermissions, FileMapping, Archiver, Packaging}


object Appsh extends Plugin {
  val defaultBinPermissions = FilePermissions(Integer.decode("0755")).getOrElse(sys.error("Invalid permissions"))

  object AppshKeys {
    val appshSource = SettingKey[File]("source for overriding defaults in project usually src/appsh")
    val appshZipDirectory = SettingKey[File]("directory that will be zipped. Usually target/appsh")
    val appshZipFile = SettingKey[File]("Zipfile. Usually target/appsh.zip")
    val appshPermissions = SettingKey[Map[String, FilePermissions]]("Map from path to unix permissions")
    val appshBuild = TaskKey[File]("appsh-build", "Creates a app.sh zip file.")
  }

  import AppshKeys._

  lazy val appshSettings: Seq[Setting[_]] = Seq(
    appshSource := baseDirectory.value / "src" / "appsh",
    appshZipDirectory := target.value / "appsh",
    appshZipFile := target.value / "appsh.zip",
    appshPermissions := Map(
      "bin/root/*" -> defaultBinPermissions ,
      "hooks/*" -> defaultBinPermissions
    ),
    appshBuild <<= (appshSource, appshZipDirectory, appshPermissions, appshZipFile, streams) map { (src, target, permissions, zip, stream) => 
      IO.delete(zip)

      val mapping = FileMapping(List(src, target), permissions = permissions)
      validate(mapping.mappings)
      val archiver = Archiver(Packaging.Zip)

      archiver.create(mapping, zip)
    }
  )

  def negate[A](p: (A) => Boolean) = (a: A) => !p(a)

  def validate(fileMap: Map[String, File]) {
    val config = fileMap.get("app.config")
    val bin = fileMap.get("root/bin")
    val postInstall = fileMap.get("hooks/post-install")
    
    val fileExists = (c: File) => c.exists && c.isFile
    val dirExists = (c: File) => c.exists && c.isDirectory

    if (!config.forall(fileExists)) {
      sys.error("Missing app.config file")
    }

    if (!bin.forall(dirExists)) {
      sys.error("Not a valid apps.sh directory, please make sure there is a 'root/bin' directory.")
      if (!fileMap.exists{case (n, f) => n.startsWith("root/bin/") && fileExists(f) }) {
        sys.error("No executables found in root/bin")
      }
    }

    if (!postInstall.forall(negate(fileExists))) {
        sys.error("'hooks/post-install' does not exist or is not a file")
    }
  }
}


