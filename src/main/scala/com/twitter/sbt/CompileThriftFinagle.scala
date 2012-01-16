package com.twitter.sbt

import sbt._
import Keys._

object CompileThriftFinagle extends Plugin {
  import CompileThrift._

  val newSettings = CompileThrift.newSettings ++ Seq(
    thriftName := "thrift-finagle"
  )
}
