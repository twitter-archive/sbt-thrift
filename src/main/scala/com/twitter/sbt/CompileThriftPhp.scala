package com.twitter.sbt

import _root_.sbt._

/**
 * Generate plain-old-php bindings
 */
trait CompileThriftPhp extends CompileThrift {
  lazy val compileThriftPhp = compileThriftAction("php")

  lazy val autoCompileThriftPhp = task {
    if (autoCompileThriftEnabled) {
      compileThriftPhp.run
    } else {
      log.info("%s: not auto-compiling thrift-php; you may need to run compile-thrift-php manually".format(name))
      None
    }
  }

  override def compileAction = super.compileAction dependsOn(autoCompileThriftPhp)
}
