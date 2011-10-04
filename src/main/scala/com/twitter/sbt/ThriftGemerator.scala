package com.twitter.sbt

import _root_.sbt._
import Process._

trait ThriftGemFactory {
  val name: String
  val namespace: String
  val service: String
  val description: String
  val authors: Seq[(String, String)]
  val homepage: String
  val repository: GemRepository = TwitterGem
  val thriftExclusions: Seq[String] = Seq()

  def apply(mainPath: Path, outputPath: Path, files: scala.collection.Set[Path], version: Version, log: Logger) =
    new ThriftGem(name, namespace, service, description, authors, homepage, repository, mainPath, outputPath, files, version, log)
}

class ThriftGem(
  name: String,
  namespace: String,
  service: String,
  desc: String,
  authors: Seq[(String,String)],
  url: String,
  repository: GemRepository,
  mainSourcePath: Path,
  outputPath: Path,
  generatedRubyFiles: scala.collection.Set[Path],
  version: Version,
  log: Logger
) {
  val basePath = mainSourcePath / "ruby" / name
  val mainLibPath = basePath / "lib"
  val libPath = mainLibPath / name
  val thriftPath = libPath / "thrift"
  val mainTestPath = basePath / "test"
  val testPath = mainTestPath / name
  val targetPath = outputPath / "gem"
  val gemName = name + "-" + version.toString.replaceAll("-SNAPSHOT","")

  def setup() = {
    FileUtilities.createDirectory(targetPath, log)
    buildDirStructure()
    createFiles()
    None
  }

  def build() = {
    copyGeneratedFiles() match {
      case None => {
        val exitCode = Process("gem build " + name + ".gemspec", basePath).run(false).exitValue()
        if (exitCode == 0) {
          (basePath / (gemName + ".gem")).asFile.renameTo((targetPath / (gemName + ".gem")).asFile)
          None
        } else {
          Some("build exit code " + exitCode)
        }
      }
      case copyRet => copyRet
    }
  }

  def release() =
    repository.release(gemName + ".gem", targetPath, log)

  private[this] def buildDirStructure() {
    log.info("Creating directory structure")
    val dirs = Seq(
      basePath,
      mainLibPath,
      libPath,
      thriftPath,
      mainTestPath,
      testPath
    )
    FileUtilities.createDirectories(dirs, log)
  }

  import scala.collection.mutable
  import java.io.File
  private[this] def copyGeneratedFiles() = {
    log.info("Setup thrift generated files")
    val gemThriftFiles = new mutable.ArrayBuffer[File]()
    generatedRubyFiles foreach { path =>
      val readFile = path.asFile
      FileUtilities.readString(readFile, log) match {
        case Left(err) => Some(err)
        case Right(str) => {
          val writeFile = (thriftPath / readFile.getName).asFile
          gemThriftFiles += writeFile
          FileUtilities.write(writeFile, fixRequires(str), log)
          None
        }
      }
    }

    FileUtilities.write(
      (libPath / "thrift.rb").asFile,
      gemThriftFiles.map { f => "require '" + name + "/thrift/" + f.getName.replaceAll("\\.rb","") + "'" }.mkString("\n"),
      log
    )
  }

  private[this] def fixRequires(str: String) = {
    str.split("\n") filter { l => l != "require 'thrift'" } map { line =>
      line.replaceAll("^require '", "require '" + name + "/thrift/")
    } mkString("\n")
  }

  private[this] val clientTemplate =
"""module @NAMESPACE@
  class Client
    def initialize(service)
      @service = service
    end
  end
end
"""

  private[this] val mockServiceTemplate =
"""module @NAMESPACE@
  class MockService
  end
end
"""

  private[this] val serviceTemplate =
"""require 'thrift_client'

module @NAMESPACE@
  class Service < ThriftClient
    def initialize(servers = nil, options = {})
      servers = if servers.nil? || servers.empty?
        ['localhost:9999']
      else
        Array(servers)
      end

      super(@NAMESPACE@::@SERVICE@::Client, servers, options)
    end
  end
end
"""

  private[this] val gemspecTemplate =
"""# -*- encoding: utf-8 -*-

top_level = `git rev-parse --show-toplevel`.chomp
Gem::Specification.new do |s|
  s.name        = "@NAME@"
  s.version     = `grep project\.version #{top_level}/project/build.properties | cut -d= -f2`.chomp.gsub("-SNAPSHOT","")
  s.platform    = Gem::Platform::RUBY
  s.authors     = [@AUTHORNAMES@]
  s.email       = [@AUTHOREMAILS@]
  s.homepage    = %q{@HOMEPAGE@}
  s.summary     = %q{@DESCRIPTION@}
  s.description = s.summary

  s.files         = `ls lib/**/*.rb`.split("\n") + `ls lib/*.rb`.split("\n")
  s.test_files    = `ls test/**/*.rb`.split("\n") + `ls test/*.rb`.split("\n")

  s.require_paths = ["lib"]

  s.add_dependency "thrift_client", ">= 0.5"
end
"""

  private[this] def createFiles() {
    log.info("Creating skeleton gem")
    FileUtilities.touch(mainLibPath / (name + ".rb"), log)

    // client.rb
    val clientRb = (libPath / "client.rb").asFile
    if (!clientRb.isFile) {
      FileUtilities.write(
        clientRb,
        clientTemplate.replaceAll("@NAMESPACE@", namespace), log)
    }

    // mock_service.rb
    val mockServiceRb = (libPath / "mock_service.rb").asFile
    if (!mockServiceRb.isFile) {
      FileUtilities.write(
        mockServiceRb,
        mockServiceTemplate.replaceAll("@NAMESPACE@", namespace), log)
    }

    // service.rb
    val serviceRb = (libPath / "service.rb").asFile
    if (!serviceRb.isFile) {
      val template = serviceTemplate
        .replaceAll("@NAMESPACE@", namespace)
        .replaceAll("@SERVICE@", service)

      FileUtilities.write(serviceRb, template, log)
    }

    val gemspec = (basePath / (name + ".gemspec")).asFile
    if (!gemspec.isFile) {
      val template = gemspecTemplate
        .replaceAll("@NAME@", name)
        .replaceAll("@AUTHORNAMES@", authors.map { case (name,_) => "%q{" + name + "}" }.mkString(","))
        .replaceAll("@AUTHOREMAILS@", authors.map { case (_,email) => "%q{" + email + "}" }.mkString(","))
        .replaceAll("@HOMEPAGE@", url)
        .replaceAll("@DESCRIPTION@", desc)

      FileUtilities.write(gemspec, template, log)
    }

    // thrift.rb (requires all the generated thrift)
    val mainFiles = Seq("thrift", "client", "mock_service", "service")
    FileUtilities.write(
      (mainLibPath / (name + ".rb")).asFile,
      mainFiles.map { f => "require '" + name + "/" + f + "'" }.mkString("\n"), log)

    // ignore files in the thrift dir
    val thriftIgnore = (thriftPath / ".gitignore").asFile
    if (!thriftIgnore.isFile)
      FileUtilities.write(thriftIgnore, "*.rb", log)

    FileUtilities.touch(mainTestPath / (name + "_test_helpers.rb"), log)
    List("client_test.rb", "mock_service_test.rb") foreach { file =>
      FileUtilities.touch(testPath / file, log)
    }
  }
}

trait GemRepository {
  def release(gemName: String, path: Path, log: Logger): Option[String]
}

object TwitterGem extends GemRepository {
  private[this] val uploadTemplate =
    """require 'rubygems'
    require 'ods_credentials'
    creds = Class.new { extend ODSCredentials }.get_keychain_data.join(':').gsub('!', '\!')
    system("curl -H\"Expect: \" -F\"file=@#{ARGV[0]}\" -u#{creds} http://gems.local.twitter.com/upload")
    """

  def release(gemName: String, path: Path, log: Logger) = {
    val uploadFile = (path / "upload.rb").asFile
    if (!uploadFile.isFile) FileUtilities.write(uploadFile, uploadTemplate, log)

    val exitCode = (("ruby " + uploadFile + " " + (path / gemName).absolutePath) !)
    if (exitCode == 0) None else Some("Failed to release to geminabox: " + exitCode)
  }
}

object RubygemGem extends GemRepository {
  def release(gemName: String, path: Path, log: Logger) = {
    val exitCode = (("gem push " + (path / gemName).absolutePath) !)
    if (exitCode == 0) None else Some("Failed to release to RubyGems: " + exitCode)
  }
}

trait ThriftGemerator extends CompileThriftRuby {
  def gemFactory: ThriftGemFactory

  lazy val gem = gemFactory(mainSourcePath, outputPath, (generatedRubyPath * "*.rb").get, version, log)

  override lazy val compileThriftRuby = compileThriftAction("rb", gemFactory.thriftExclusions)

  lazy val gemerate = task {
    gem.setup()
  } describedAs("Create the gem structure")

  override def updateAction = gemerate dependsOn(super.updateAction)

  lazy val buildGem = task {
    gem.build()
  } dependsOn(compileThriftRuby) describedAs("Build the gem")

  lazy val releaseGem = task {
    gem.release()
  } dependsOn(buildGem) describedAs("Release gem")
}
