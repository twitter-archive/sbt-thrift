import java.io.{File, FileReader, FileWriter}
import java.util.{Date, Properties}
import _root_.sbt._

// TODO: somehow link on the real SubversionPublisher in the main source tree
class SbtThriftPlugin(info: ProjectInfo) extends PluginProject(info) with SubversionPublisher with IdeaProject {
  override def disableCrossPaths = true

  override def subversionRepository = Some("http://svn.local.twitter.com/maven-public")

  val ivySvn = "ivysvn" % "ivysvn" % "2.1.0" from "http://maven.twttr.com/ivysvn/ivysvn/2.1.0/ivysvn-2.1.0.jar"
  val jruby = "org.jruby" % "jruby-complete" % "1.6.0.RC2"

  override def managedStyle = ManagedStyle.Maven
  def artifactoryRoot = "http://artifactory.local.twitter.com"
  def snapshotDeployRepo = "libs-snapshots-local"
  def releaseDeployRepo = "libs-releases-local"

  override def pomExtra =
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
}
