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
  val testFramework: Option[GemTestFramework] = None
  val thriftExclusions: Seq[String] = Seq()

  def apply(mainPath: Path, outputPath: Path, files: PathFinder, version: Version, log: Logger) =
    new ThriftGem(name, namespace, service, description, authors, homepage, repository, testFramework,
                  mainPath, outputPath, files, version, log)
}

class ThriftGem(
  name: String,
  namespace: String,
  service: String,
  desc: String,
  authors: Seq[(String,String)],
  url: String,
  repository: GemRepository,
  testFramework: Option[GemTestFramework],
  mainSourcePath: Path,
  outputPath: Path,
  generatedRubyFiles: PathFinder,
  version: Version,
  log: Logger
) {
  val basePath = mainSourcePath / "ruby" / name
  val mainLibPath = basePath / "lib"
  val libPath = mainLibPath / name
  val thriftPath = libPath / "thrift"
  val targetPath = outputPath / "gem"
  val gemName = name + "-" + version.toString.replaceAll("-SNAPSHOT","")

  def setup() = {
    createFiles() match {
      case None => testFramework.map(_.setup(name, basePath, log)).getOrElse(None)
      case err  => err
    }
  }

  def build() = {
    copyGeneratedFiles() match {
      case None => {
        val exitCode = (Process("gem build " + name + ".gemspec", basePath) !)
        if (exitCode == 0) {
          FileUtilities.createDirectory(targetPath, log)
          (basePath / (gemName + ".gem")).asFile.renameTo((targetPath / (gemName + ".gem")).asFile)
          None
        } else {
          Some("build exit code " + exitCode)
        }
      }
      case copyRet => copyRet
    }
  }

  def test() = {
    testFramework.map(_.test(basePath, log)).getOrElse(None)
  }

  def release() = {
    FileUtilities.createDirectory(targetPath, log)
    repository.release(gemName + ".gem", targetPath, log)
  }

  import scala.collection.mutable
  import java.io.File
  private[this] def copyGeneratedFiles() = {
    log.info("Setup thrift generated files")
    val gemThriftFiles = new mutable.ArrayBuffer[File]()
    generatedRubyFiles.get foreach { path =>
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
"""module @NAMESPACE@
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

  s.files         = Dir["lib/**/*.rb"].to_a
  s.test_files    = Dir["test/**/*.rb"].to_a + Dir["spec/**/*.rb"].to_a

  s.require_paths = ["lib"]

  s.add_dependency "thrift_client", ">= 0.5"
end
"""

  private[this] def createFiles() = {
    log.info("Creating directory structure")
    val dirs = Seq(basePath, mainLibPath, libPath, thriftPath, targetPath)
    FileUtilities.createDirectories(dirs, log)
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
    val mainFiles = Seq("thrift_client") ++ Seq("thrift", "client", "mock_service", "service").map(name + "/" + _)
    FileUtilities.write(
      (mainLibPath / (name + ".rb")).asFile,
      mainFiles.map { f => "require '" + f + "'" }.mkString("\n"), log)

    // ignore files in the thrift dir
    val thriftIgnore = (thriftPath / ".gitignore").asFile
    if (!thriftIgnore.isFile)
      FileUtilities.write(thriftIgnore, "*.rb", log)

    None
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

trait GemTestFramework {
  def test(basePath: Path, log: Logger): Option[String]
  def setup(name: String, basePath: Path, log: Logger): Option[String]
}

object GemMinitest extends GemTestFramework {
  private[this] val rakefileTemplate =
    """
require 'rake/testtask'

Rake::TestTask.new(:test) do |test|
  test.libs << 'test'
  test.pattern = 'test/**/test_*.rb'
  test.verbose = true
end"""

  private[this] val helperTemplate =
    """require 'rubygems'
require 'minitest/unit'
require 'minitest/mock'

require '@NAME@'

MiniTest::Unit.autorun
"""

  private[this] val mockTemplate =
    """require '@NAME@_test_helper'

class MockServiceTest < MiniTest::Unit::TestCase
end"""

  private[this] val clientTemplate =
    """require '@NAME@_test_helper'

class ClientTest < MiniTest::Unit::TestCase
end"""

  def test(basePath: Path, log: Logger) = {
    val exitCode = (Process("rake test", basePath) !)
    if (exitCode == 0) None else Some("Gem tests failed")
  }

  def setup(name: String, basePath: Path, log: Logger) = {
    val rakefile = (basePath / "Rakefile").asFile
    if (!rakefile.isFile) FileUtilities.write(rakefile, rakefileTemplate, log)

    val mainTestPath = basePath / "test"
    val testPath = mainTestPath / name
    FileUtilities.createDirectories(Seq(mainTestPath, testPath), log)

    val helper = (mainTestPath / (name + "_test_helper.rb")).asFile
    if (!helper.isFile) FileUtilities.write(helper, helperTemplate.replaceAll("@NAME@", name), log)

    val mockTest = (testPath / "test_mock_service.rb").asFile
    if (!mockTest.isFile) FileUtilities.write(mockTest, mockTemplate.replaceAll("@NAME@", name), log)

    val clientTest = (testPath / "test_client.rb").asFile
    if (!clientTest.isFile) FileUtilities.write(clientTest, clientTemplate.replaceAll("@NAME@", name), log)

    None
  }
}

trait ThriftGemerator extends CompileThriftRuby {
  def gemFactory: ThriftGemFactory

  lazy val gem = gemFactory(mainSourcePath, outputPath, (generatedRubyPath * "*.rb"), version, log)

  override lazy val compileThriftRuby = compileThriftAction("rb", gemFactory.thriftExclusions)

  lazy val gemerate = task {
    gem.setup()
  } describedAs("Create the gem structure")

  override def updateAction = gemerate dependsOn(super.updateAction)

  lazy val cleanGem = (
    cleanTask(gem.targetPath) && cleanTask(gem.thriftPath ** "*.rb")
  ) describedAs("Clean generated gem folders")

  override def cleanAction = super.cleanAction dependsOn(cleanGem)

  lazy val buildGem = task {
    gem.build()
  } dependsOn(compileThriftRuby) describedAs("Build the gem")

  lazy val testGem = task {
    gem.test()
  } dependsOn(buildGem) describedAs("Run the gem's test suite")

  lazy val releaseGem = task {
    gem.release()
  } dependsOn(testGem) describedAs("Release gem")
}
