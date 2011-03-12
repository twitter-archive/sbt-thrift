package com.twitter.sbt

import _root_.sbt._

trait GeneratedSources extends DefaultProject {
  def generatedJavaDirectoryName   = "gen-java"
  def generatedRubyDirectoryName   = "gen-rb"
  def generatedPythonDirectoryName = "gen-py"

  def generatedJavaPath   = outputPath / generatedJavaDirectoryName
  def generatedRubyPath   = outputPath / generatedRubyDirectoryName
  def generatedPythonPath = outputPath / generatedPythonDirectoryName

  override def mainSourceRoots = super.mainSourceRoots +++ (outputPath / generatedJavaDirectoryName ##)

  lazy val cleanGenerated = (
      cleanTask(generatedJavaPath) && cleanTask(generatedRubyPath) && cleanTask(generatedPythonPath)
    ) describedAs "Clean generated source folders"

  override def cleanAction = super.cleanAction dependsOn(cleanGenerated)
}
