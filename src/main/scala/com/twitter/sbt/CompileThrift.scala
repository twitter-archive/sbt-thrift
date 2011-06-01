package com.twitter.sbt

import _root_.sbt._
import java.io.File

import scala.collection.jcl

trait CompileThrift extends DefaultProject with GeneratedSources {
  def thriftBin = jcl.Map(System.getenv()).get("THRIFT_BIN").getOrElse("thrift")

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
