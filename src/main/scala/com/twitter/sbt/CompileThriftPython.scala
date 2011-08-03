package com.twitter.sbt

import _root_.sbt._

/**
 * Generate plain-old-Python bindings
 */
trait CompileThriftPython extends CompileThrift {
  @Deprecated
  val compileThriftPythonTwistedEnabled = false
  lazy val pythonThriftSpec = {
    if (compileThriftPythonTwistedEnabled) "py:new_style,twisted"
    else "py:new_style"
  }
  lazy val compileThriftPython = compileThriftAction(pythonThriftSpec)

  lazy val autoCompileThriftPython = task {
    if (autoCompileThriftEnabled) {
      compileThriftPython.run
    } else {
      log.info("%s: not auto-compiling thrift-python; you may need to run compile-thrift-python manually".format(name))
      None
    }
  }

  override def compileAction = super.compileAction dependsOn(autoCompileThriftPython)
}

/**
 * Generate Twisted Python bindings
 */
trait CompileThriftPythonTwisted extends CompileThrift {
  lazy val compileThriftPythonTwisted = compileThriftAction("py:new_style,twisted")

  lazy val autoCompileThriftPythonTwisted = task {
    if (autoCompileThriftEnabled) {
      compileThriftPythonTwisted.run
    } else {
      log.info("%s: not auto-compiling thrift-python-twisted; you may need to run compile-thrift-python-twisted manually".format(name))
      None
    }
  }

  override def compileAction = super.compileAction dependsOn(autoCompileThriftPythonTwisted)
}
