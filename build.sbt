import com.twitter.sbt._

organization := "com.twitter"

name := "sbt-thrift2"

version := "0.0.1-SNAPSHOT"

sbtPlugin := true

seq(StandardProject.newSettings: _*)

seq(SubversionPublisher.newSettings: _*)

SubversionPublisher.subversionRepository := Some("https://svn.twitter.biz/maven-public")

seq(ScriptedPlugin.scriptedSettings: _*)
