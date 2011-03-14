package com.twitter.sbt

import _root_.sbt._

trait CompileThriftJava extends CompileThrift {
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
