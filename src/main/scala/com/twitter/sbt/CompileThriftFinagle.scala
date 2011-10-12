package com.twitter.sbt

import _root_.sbt._
import java.io.File

trait CompileThriftFinagle
  extends DefaultProject with CompileThrift
{
  override lazy val thriftname = "thrift-finagle"
  override def compileAction = super.compileAction dependsOn(autoCompileThriftJava)
}
