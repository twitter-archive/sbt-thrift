package com.twitter.sbt

import _root_.sbt._


trait CompileThriftRuby extends CompileThrift {
  lazy val compileThriftRuby = compileThriftAction("rb")

  lazy val autoCompileThriftRuby = task {
    if (autoCompileThriftEnabled) {
      compileThriftRuby.run
    } else {
      log.info("%s: not auto-compiling thrift-ruby; you may need to run compile-thrift-java manually".format(name))
      None
    }
  }

  override def compileAction = super.compileAction dependsOn(autoCompileThriftRuby)
}
