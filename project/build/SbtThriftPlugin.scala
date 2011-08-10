import sbt._
import com.twitter.sbt._

class SbtThriftPlugin(info: ProjectInfo) extends PluginProject(info)
    with StandardManagedProject with DefaultRepos with SubversionPublisher {
  override def disableCrossPaths = true

  val jruby = "org.jruby" % "jruby-complete" % "1.6.0.RC2"

  override def subversionRepository = Some("http://svn.local.twitter.com/maven-public")
  override def managedStyle = ManagedStyle.Maven
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
