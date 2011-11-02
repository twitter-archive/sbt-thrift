package com.twitter.sbt

import _root_.sbt._

trait GeneratedSources extends DefaultProject {
  def generatedJavaDirectoryName   = "gen-java"
  def generatedRubyDirectoryName   = "gen-rb"
  def generatedPhpDirectoryName    = "gen-php"
  def generatedPythonDirectoryName = "gen-py"
  def generatedScalaDirectoryName = "gen-scala"

  def generatedJavaPath   = outputPath / generatedJavaDirectoryName
  def generatedRubyPath   = outputPath / generatedRubyDirectoryName
  def generatedPhpPath    = outputPath / generatedPhpDirectoryName
  def generatedPythonPath = outputPath / generatedPythonDirectoryName
  def generatedScalaPath = outputPath / generatedScalaDirectoryName

  override def mainSourceRoots = super.mainSourceRoots +++ (outputPath / generatedJavaDirectoryName ##)

  lazy val cleanGenerated = (
      cleanTask(generatedJavaPath) && cleanTask(generatedRubyPath)
        && cleanTask(generatedPythonPath) && cleanTask(generatedScalaDirectoryName)
        && cleanTask(generatedPhpPath)
    ) describedAs "Clean generated source folders"

  override def cleanAction = super.cleanAction dependsOn(cleanGenerated)
}
