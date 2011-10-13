package com.twitter.sbt

import _root_.sbt._

trait CompileThriftJava extends CompileThrift {
  override def compileAction = super.compileAction dependsOn(autoCompileThriftJava)
}
