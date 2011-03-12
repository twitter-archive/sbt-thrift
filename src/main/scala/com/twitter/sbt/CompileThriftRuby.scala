package com.twitter.sbt

import _root_.sbt._


trait CompileThriftRuby extends CompileThrift {
  lazy val compileThriftRuby = compileThriftAction("rb") describedAs("Compile thrift into ruby")

  lazy val autoCompileThriftRuby = task {
    if (autoCompileThriftEnabled) compileThriftRuby.run
    else {
      log.info(name+": not auto-compiling thrift-ruby; you may need to run compile-thrift-ruby manually")
      None
    }
  }

  override def compileAction = super.compileAction dependsOn(autoCompileThriftRuby)
}
