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
    val appshPermissions = TaskKey[Map[String, Int]]("appsh-permissons", "Map from path to unix permissions")
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
      
      val filesystem = buildFilesystem(target, src)
      val perms = preparePermissions(filesystem, permissions)

      makeZip(filesystem, perms, zip, stream)
    }
  )

  def buildFilesystem(target: File, src: File) = {
    def entries(f: File): List[File] = f :: (if (f.isDirectory) IO.listFiles(f).toList.flatMap(entries) else Nil)
    def tupled(root: File) = (f: File) => f.getAbsolutePath.substring(root.toString.length).substring(1) -> f
    val withoutTarget = entries(target).tail.map(tupled(target)).toMap
    val withoutSrc = entries(src).tail.map(tupled(src)).toMap

    withoutTarget ++ withoutSrc
  }

  def preparePermissions(filesystem: Map[String, File], permissions: Map[String, Int]) = {
    permissions.flatMap { case (path, perm) => 
      val actualKey = {
        val noStar = if (path.endsWith("*")) path.substring(0, path.length - 1) else path
        if (noStar.startsWith("/")) noStar.substring(1) else noStar
      }
      filesystem.flatMap {case (path2, _) => if (path2.startsWith(actualKey)) Map(path2 -> perm) else Map.empty[String, Int]}
    }.toMap
  }

  def makeZip(fileMap: Map[String, File], permissionMap: Map[String, Int], zipFile: File, stream: TaskStreams) = {
    validate(fileMap)
    val containerDirectory = zipFile.getAbsoluteFile.getParentFile
    if (!containerDirectory.exists) {
      IO.createDirectory(containerDirectory)
    }

    val entries = fileMap.map { case (path, f) =>
      val e = new ZipArchiveEntry(f, path)
      permissionMap.get(path).foreach(p => e.setUnixMode(p))
      (f, e)
    }

    val os = new ZipArchiveOutputStream(zipFile)
    
    entries.foreach{case (f, e) =>
      os.putArchiveEntry(e)
      if (f.isFile) {
        IO.transfer(f, os)
      }
      os.closeArchiveEntry()
    }
    os.close()

    stream.log.info("Wrote " + zipFile)
    zipFile
  }

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
