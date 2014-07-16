package appmgr

import sbt._
import Keys._
import archiver.{FilePermissions, FileMapping, Archiver, Packaging}


object Appmgr extends Plugin {
  val defaultBinPermissions = FilePermissions(Integer.decode("0755")).getOrElse(sys.error("Invalid permissions"))
  val Appmgr = config("appmgr")

  val appmgrOutputFile = SettingKey[File]("Zipfile. Default target/appmgr.zip")
  val appmgrPermissions = SettingKey[Map[String, FilePermissions]]("Map from path to unix permissions")
  val appmgrBuild = TaskKey[File]("appmgr-build", "Create the appmgr distribution")
  val appmgrLauncher = SettingKey[Option[Launcher]]("""|Default Launcher:
    |can be loaded from classpath by adding a classpath:/path/to/launcher.
    |or from file by using file:/path/to/file.
    |The file can expect a few config parameters:
    | - launcher.command - command to run
    | - launcher.name - Name of the app
    | - launcher.description - Short desciption of program
    |
    |The app.config file will be auto-generated if this is set.
    |The auto-generated file will be merged any existing app.config file.
    |
    |The default implementation will expect a jvm program,
    |so we will register a JAVA_OPTS and a JVM_OPT environment variable.
    |
    |Config variables registered in the 'app.name' config group
    |will be passed on as system properties.
    |
    |Example app.config:
    | app.launcher=bin/launcher.sh
    | launcher.command=main
    | launcher.name=foo
    | launcher.desciption=Foo program
    | foo.server=example.com
    |
    """.stripMargin)

  val appmgrSettings: Seq[Setting[_]] = inConfig(Appmgr)(Seq(
    sourceDirectory := baseDirectory.value / "src" / "appmgr",
    managedDirectory := target.value / "appmgr",
    appmgrOutputFile := target.value / "appmgr.zip",
    appmgrPermissions := Map(
      "root/bin/*" -> defaultBinPermissions ,
      "hooks/*" -> defaultBinPermissions
    ),
    appmgrLauncher <<= (name, description).apply{ (n, d) =>
      Some(Launcher(Resource.DefaultLauncher, "main", n, d))
    },
    appmgrBuild <<= (sourceDirectory, managedDirectory, appmgrLauncher, appmgrPermissions, appmgrOutputFile, streams) map { (src, target, launcher, permissions, zip, stream) =>
      if (zip.exists) IO.delete(zip)
      IO.withTemporaryDirectory{ temp =>
        val mapping = FileMapping(List(target, src), permissions = permissions)
        val launcherM = handleLauncher(launcher, mapping, temp)
        val real = mapping.append(launcherM)
        validate(mapping)
        val archiver = Archiver(Packaging(zip))
        archiver.create(real, zip)
      }
    },
    Keys.`package` <<= appmgrBuild
  )) ++ Seq(
    appmgrBuild <<= appmgrBuild in Appmgr
  )

  def handleLauncher(launcher: Option[Launcher], mapping: FileMapping, directory: File): FileMapping = {
    import collection.JavaConverters._

    def config(l: Launcher) = {
      val config = mapping.mappings.get("app.config")
      val configMap = config.foldLeft(new java.util.Properties()){case (p, f) => 
        val is = new java.io.FileInputStream(f)
        p.load(is)
        is.close()
        p
      }.asScala.toMap
      val c = if (!configMap.contains("app.launcher")) configMap ++ l.asMap else configMap
      c.map{case (k,v) => s"$k=$v"}.mkString("", "\n", "\n")
    }

    val map = launcher match {
      case Some(l) => {
        val target = directory / "launcher.sh"
        IO.download(l.launcher.asURL, target)
        val configFile = directory / "app.config"       
        IO.write(configFile, config(l))
        val m = Map(
          "root/bin/launcher.sh" -> target,
          "app.config" -> configFile
        )
        FileMapping(m, Map("root/bin/launcher.sh" -> defaultBinPermissions))
      }
      case None => FileMapping(Map.empty[String, File], Map.empty[String, FilePermissions]) 
    }
    map
  }

  def negate[A](p: (A) => Boolean) = (a: A) => !p(a)

  def validate(mapping: FileMapping) {
    val fileMap = mapping.mappings
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


