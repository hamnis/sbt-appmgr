package appsh

import java.net.URL
import java.io.File

sealed trait Resource {
  def asURL: URL
}

object Resource {
  val FileRegex = "file:(.*)".r
  val ClasspathRegex = "classpath:(.*)".r
  val DefaultLauncher = Resource("classpath:/launcher/default-launcher.sh")

  class FileResource private[Resource](file: File) extends Resource {
    def asURL = file.toURI.toURL
    override def toString = "file:" + file.toString
  }

  class ClasspathResource private[Resource](url: URL) extends Resource {
    def asURL = url
    override def toString = "classpath:" + url.getPath
  }

  def apply(input: String) = {
    input match {
      case FileRegex(_) => new FileResource(new File(input))
      case ClasspathRegex(path) => {
        val actualPath = if(path.startsWith("/")) path.drop(1) else path
        new ClasspathResource(getClass.getClassLoader().getResource(actualPath))
      }
      case _ => throw new IllegalArgumentException("Unknown scheme: " + input)
    }
  }

  def unapply(r: Resource) = Some(r.asURL)
}


case class Launcher(launcher: Resource, command: String, name: String, description: String) {
  def asMap = Map(
    "app.launcher" -> "bin/launcher.sh",
    "launcher.command" -> command,
    "launcher.name" -> name,
    "launcher.description" -> description
  )
}
