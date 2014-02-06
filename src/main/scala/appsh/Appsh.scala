package appsh

import sbt._
import Keys._
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream}

object Appsh extends Plugin {
  object AppshKeys {
    val appshSource = SettingKey[File]("source for overriding defaults in project usually src/appsh")
    val appshZipDirectory = SettingKey[File]("directory that will be zipped. Usually target/appsh")
    val appshZipFile = SettingKey[File]("Zipfile. Usually target/appsh.zip")
    val appshBuild = TaskKey[File]("appsh-build", "Creates a app.sh zip file.")
  }

  import AppshKeys._

  lazy val appshSettings: Seq[Setting[_]] = Seq(
    appshSource := baseDirectory.value / "src" / "appsh",
    appshZipDirectory := target.value / "appsh",
    appshZipFile := target.value / "appsh.zip",
    appshBuild <<= (appshSource, appshZipDirectory, appshZipFile, streams) map { (src, target, zip, stream) => 
      IO.delete(zip)
      IO.delete(target)
      prepareZipDirectory(src, target)
      makeZip(target, zip, stream)
    }
  )

  def prepareZipDirectory(src: File, target: File) {
    val root = target / "root"
    IO.createDirectories(Seq(target, root))
    IO.copyDirectory(src, target, overwrite = false)
    ()
  }

  def makeZip(destDir: File, zipFile: File, stream: TaskStreams) = {    
    validate(destDir)

    def entries(f: File):List[File] = f :: (if (f.isDirectory) IO.listFiles(f).toList.flatMap(entries) else Nil)

    def zip(files: Seq[File], file: File) {
      val entries = files.map{ f =>
        val path = f.getAbsolutePath.substring(destDir.toString.length)
        val e = new ZipArchiveEntry(f, if (path.startsWith("/")) path.substring(1) else path)
        if (path.contains("bin") || path.contains("hooks") && f.isFile) {
          e.setUnixMode(Integer.decode("0755"))
        }
        (f, e)
      }

      val os = new ZipArchiveOutputStream(file)
      entries.foreach{case (f, e) =>
        os.putArchiveEntry(e)
        if (!f.isDirectory) {
          IO.transfer(f, os)
        }
        os.closeArchiveEntry()
      }
      os.close()
    }
    
    zip(entries(destDir).tail, zipFile)
    stream.log.info("Wrote " + zipFile)
    zipFile
  }

  def validate(appshDir: File) = {
    val config = appshDir / "app.config"
    val root = appshDir / "root"
    val bin = root / "bin"
    val hooks = appshDir / "hooks"
    
    if (!config.exists && !config.isFile) {
      sys.error("Missing app.config file")
    }

    if (!root.exists && !root.isDirectory) {
      sys.error("Not a valid apps.sh directory, please make sure there is a 'root' directory.")
    }
    if (hooks.exists) {
      val postInstall = hooks / "post-install"
      if (!postInstall.exists && !postInstall.isFile) {
        sys.error(s"$postInstall does not exist or is not a file")
      }
    }
    if (bin.exists && bin.isDirectory) {
      val files = bin.listFiles()
      if (files == null || files.isEmpty) {
       sys.error("There are no start scripts for your app")
      }
    }
    else {
      sys.error("Not a valid apps.sh directory, please make sure there is a 'bin' directory in 'root'.")
    }
  }

}
