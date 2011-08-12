package com.twitter.sbt

import java.io.InputStreamReader
import _root_.sbt._
import org.jruby.embed._

class PartiallySpecifiedNamespace(ruby: Option[String], java: Option[String]) {
  def fromRuby(ns: String) = new PartiallySpecifiedNamespace(Some(ns), java)
  def fromJava(ns: String) = new PartiallySpecifiedNamespace(ruby, Some(ns))
  def toScala(ns: String) = {
    if(!ruby.isDefined) throw new RuntimeException("Please specify the ruby namespace from your thrift file")
    if(!java.isDefined) throw new RuntimeException("Please specify the java namespace from your thrift file")
    new ThriftNamespace(ruby.get, java.get, ns)
  }
}

object ThriftNamespace extends PartiallySpecifiedNamespace(None, None)

case class ThriftNamespace(ruby: String, java: String, scala: String) {
  def this(ruby: String, scala: String) = this(ruby, "%s.thrift".format(scala), scala)
}

/**
 * This code compiles scala wrappers for thrift. I can't figure out how to test inside this
 * package (recursive self sbt plugin tests?!), so I created a sample twitter-local project
 * called "quack", that has an extremely heinous thrift IDL, which exercises every thrift type
 * (with nesting and structs).  Grab it and compile it, to make sure this still works :).
 */
trait CompileThriftScala extends DefaultProject with CompileThriftFinagle with CompileThriftRuby {
  @Deprecated
  def scalaThriftTargetNamespace: String =
    throw new RuntimeException("Please override def scalaThriftTargetNamespace or thriftNamespaces (latter preferred)")

  @Deprecated
  def rubyThriftNamespace: String =
    throw new RuntimeException("Please override def rubyThriftNamespace or originalThriftNamespaces (latter preferred)")
  @Deprecated
  def javaThriftNamespace = scalaThriftTargetNamespace + ".thrift"
  @Deprecated
  def originalThriftNamespaces = Map(rubyThriftNamespace->javaThriftNamespace)

  def thriftExceptionWrapperClass = ""

  def thriftNamespaces: List[ThriftNamespace] = originalThriftNamespaces.map { case (r,j) =>
    ThriftNamespace(r, j, scalaThriftTargetNamespace)
  }.toList

  def idiomizeMethods = true

  lazy val compileThriftScala = task {
    val name = "/ruby/codegen.rb"
    val stream = getClass.getResourceAsStream(name)
    val reader = new InputStreamReader(stream)
    val container = new ScriptingContainer(LocalContextScope.SINGLETON, LocalVariableBehavior.TRANSIENT)
    container.runScriptlet(reader, "__TMP__")
    val module = container.runScriptlet("Codegen")
    thriftNamespaces.foreach { case ThriftNamespace(_rubyNs, _javaNs, _targetNs) =>
      container.callMethod(module, "run",
        (outputPath / generatedRubyDirectoryName ##).toString,
        (outputPath / generatedScalaDirectoryName ##).toString,

        _javaNs, _rubyNs, _targetNs,
        thriftExceptionWrapperClass,
        new java.lang.Boolean(idiomizeMethods))
    }
    None
  }

  /**
   * Avoid the spinning ~compile
   */
  override def watchPaths = super.watchPaths.filter { path =>
    !path.asFile.getAbsolutePath.contains("target/gen-")
  }

  lazy val autoCompileScalaThrift = task {
    if (autoCompileThriftEnabled) {
      compileThriftScala.run
    } else {
      log.info("%s: not auto-compiling thrift-scala; you may need to run compile-thrift-scala manually".format(name))
    }
    None
  }.dependsOn(autoCompileThriftRuby)

  def generatedScalaDirectoryName = "gen-scala"
  def generatedScalaPath = outputPath / generatedScalaDirectoryName

  override def mainSourceRoots = super.mainSourceRoots +++ (outputPath / generatedScalaDirectoryName ##)
  override def compileAction = super.compileAction dependsOn(autoCompileScalaThrift)
  override def cleanAction = super.cleanAction dependsOn(cleanTask(generatedScalaPath))
}
