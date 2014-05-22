import AppshKeys._

name := "basic"

appshSettings

appshZipFile := target.value / "appsh-build"

val copyToLib = taskKey[Unit]("Copy to lib")

copyToLib <<= { (packageBin in Compile, dependencyClasspath in Compile, target) map { (bin, cp, dest) =>
    val lib = dest / "appsh" / "root" / "lib"
    IO.createDirectory(lib)
    IO.copyFile(bin, lib / bin.getName)
    cp.filter(a => sbt.classpath.ClasspathUtilities.isArchive(a.data)).foreach{ a => 
      val f = a.data
      IO.copyFile(f, lib / f.getName)
    }
  }
}

appshBuild <<= appshBuild.dependsOn(copyToLib)
