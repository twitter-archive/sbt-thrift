package com.twitter.sbt

import _root_.sbt._
import java.io.File

trait CompileThriftFinagle
  extends DefaultProject with CompileThrift
{
  // thrift generation.
  override def compileThriftAction(lang: String) = task {
    import Process._
    outputPath.asFile.mkdirs()

    val thriftIncludes = thriftIncludeFolders.map { folder =>
      "-I " + new File(folder).getAbsolutePath
    }.mkString(" ")

    val tasks = thriftSources.getPaths.map { path =>
      execTask { "%s %s --gen %s -o %s %s".format(thriftBinFinagle, thriftIncludes, lang, outputPath.absolutePath, path) }
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

  override def compileAction = super.compileAction dependsOn(autoCompileThriftJava)
}
