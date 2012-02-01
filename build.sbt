organization := "com.twitter"

name := "sbt-thrift2"

version := "0.0.1-SNAPSHOT"

sbtPlugin := true

seq(ScriptedPlugin.scriptedSettings: _*)

credentials += Credentials(Path.userHome / ".artifactory-credentials")

publishTo <<= (version) { version: String =>
  val artifactory = "http://artifactory.local.twitter.com/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at artifactory + "libs-snapshots-local/")
  else                                   Some("releases"  at artifactory + "libs-releases-local/")
}
