package com.twitter.sbt

import java.io.{File, FileOutputStream, BufferedOutputStream}
import _root_.sbt._

object CompileThriftFinagle {
  var cachedPath: Option[String] = None
}

trait CompileThriftFinagle
  extends DefaultProject
  with CompileThriftJava
{
  import CompileThriftFinagle._

  private[this] val _thriftBin = CompileThriftFinagle.synchronized {
    if (!cachedPath.isDefined) {
      // TODO: we don't discriminate between versions here (which we need to..).
      val binPath = System.getProperty("os.name") match {
        case "Mac OS X" => "thrift.osx10.6"
        case "Linux" => System.getProperty("os.arch") match {
          case "i386" => "thrift.linux32"
          case "amd64" => "thrift.linux64"
          case arch => throw new Exception(
            "No thrift linux binary for %s, talk to william@twitter.com".format(arch))
        }
        case "FreeBSD" => System.getProperty("os.arch") match {
          case "amd64" => "thrift.bsd64"
          case arch => throw new Exception(
            "No thrift BSD binary for %s, talk to brandon@twitter.com".format(arch))
        }
        case unknown => throw new Exception(
          "No thrift binary for %s, talk to marius@twitter.com".format(unknown))
      }

      val stream = getClass.getResourceAsStream("/thrift/%s".format(binPath))
      val file = File.createTempFile("thrift", "scala")
      file.deleteOnExit()
      val fos = new BufferedOutputStream(new FileOutputStream(file), 1<<20)
      try {
        var byte = stream.read()
        while (byte != -1) {
          fos.write(byte)
          byte = stream.read()
        }
      } finally {
        fos.close()
      }

      import Process._
      val path = file.getAbsolutePath()
      (execTask { "chmod 0500 %s".format(path) }).run

      cachedPath = Some(path)
    }

    cachedPath.get
  }

  override def thriftBin = _thriftBin
}
