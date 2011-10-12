package com.twitter.sbt

import _root_.sbt._
import java.io.File

import scala.collection.jcl
import java.io.{File, FileOutputStream, BufferedOutputStream}

// TODO support multiple thrift versions
object CompileThrift {
  var cachedThriftPath: Option[(String, String)] = None
}

trait CompileThrift extends DefaultProject with GeneratedSources {
  import CompileThrift._
  
  private[this] val env = jcl.Map(System.getenv())

  // name of the thrift binary
  lazy val thriftname = "thrift"

  private[this] lazy val platform =  System.getProperty("os.name") match {
    case "Mac OS X" => "osx10.6"
    case "Linux" => System.getProperty("os.arch") match {
      case "i386" => "linux32"
      case "amd64" => "linux64"
      case arch => throw new Exception(
        "No thrift linux binary for %s, talk to william@twitter.com".format(arch))
    }
    case "FreeBSD" => System.getProperty("os.arch") match {
      case "amd64" => "bsd64"
      case arch => throw new Exception(
        "No thrift BSD binary for %s, talk to brandon@twitter.com".format(arch))
    }
    case unknown => throw new Exception(
      "No thrift binary for %s, talk to marius@twitter.com".format(unknown))
  }

  private[this] def extractBinary(binPath: String) = {
    val stream = getClass.getResourceAsStream("/thrift/%s".format(binPath))
    val file = File.createTempFile(binPath, "")
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
    path
  }

  lazy val thriftBin = env.get("SBT_THRIFT_BIN") getOrElse
    CompileThrift.synchronized {
      val cached = for {
        (cachedThrift, cachedPath) <- cachedThriftPath
        if cachedThrift == thriftname
      } yield cachedPath
      
      cached getOrElse {
        val path = extractBinary(thriftname + "." + platform)
        cachedThriftPath = Some((thriftname, path))
        path
      }
    }

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

  lazy val compileThriftJava = compileThriftAction("java")
  lazy val autoCompileThriftJava = task {
    if (autoCompileThriftEnabled) {
      compileThriftJava.run
    } else {
      log.info("%s: not auto-compiling thrift-java; you may need to run compile-thrift-java manually".format(name))
      None
    }
  }
}
