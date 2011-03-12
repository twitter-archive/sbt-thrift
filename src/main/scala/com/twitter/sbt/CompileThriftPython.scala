package com.twitter.sbt

import _root_.sbt._


trait CompileThriftPython extends CompileThrift {
  lazy val compileThriftPython =
    compileThriftAction("py:new_style") describedAs("Compile thrift into python")

  lazy val autoCompileThriftPython = task {
    if (autoCompileThriftEnabled)
      compileThriftPython.run
    else {
      log.info(name+": not auto-compiling thrift-python; you may need to run compile-thrift-python manually")
      None
    }
  }

  override def compileAction = super.compileAction dependsOn(autoCompileThriftPython)
}

trait CompileThriftPythonTwisted extends CompileThriftPython {
  override lazy val compileThriftPython =
    compileThriftAction("py:new_style,twisted") describedAs("Compile thrift into twisted python")
}
