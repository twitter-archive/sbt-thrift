package com.twitter.sbt

import _root_.sbt._
import java.io.File

import scala.collection.jcl
import java.io.{File, FileOutputStream, BufferedOutputStream}


object CompileThrift {
  var cachedPath: Option[String] = None
}


trait CompileThrift extends DefaultProject with GeneratedSources {
  import CompileThrift._

  private[this] val _thriftBin = CompileThrift.synchronized {
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
        // TODO(oliver): upgrade to 2.8 so that i can declare @scala.annotation.tailrec
        def copy(out: java.io.OutputStream, in: java.io.InputStream) {
          val buf = new Array[Byte](4096)
          val len = in.read(buf)
          if(len > 0) {
            out.write(buf, 0, len)
            copy(out, in)
          }
        }

        copy(fos, stream)
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

  def thriftBin = _thriftBin

  def thriftSources = (mainSourcePath / "thrift" ##) ** "*.thrift"

  /** override to disable auto-compiling of thrift */
  def autoCompileThriftEnabled = true

  def thriftIncludeFolders: Seq[String] = Nil

  // thrift generation.
  def compileThriftAction(lang: String) = task {
    import Process._
    outputPath.asFile.mkdirs()

    val thriftIncludes = thriftIncludeFolders.map { folder =>
      "-I " + new File(folder).getAbsolutePath
    }.mkString(" ")

    val tasks = thriftSources.getPaths.map { path =>
      execTask { "%s %s --gen %s -o %s %s".format(thriftBin, thriftIncludes, lang, outputPath.absolutePath, path) }
    }
    if (tasks.isEmpty) None else tasks.reduceLeft { _ && _ }.run
  } describedAs("Compile thrift into %s".format(lang))
}
