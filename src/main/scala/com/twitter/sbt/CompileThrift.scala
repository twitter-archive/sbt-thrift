package com.twitter.sbt

import sbt._
import Keys._

import java.io.File

import scala.collection.mutable.Map
import scala.collection.JavaConversions._
import java.io.{File, FileOutputStream, BufferedOutputStream}

// TODO support multiple thrift versions
object CompileThrift extends Plugin {
  var cachedThriftPath: Option[(String, File)] = None
  
  val environment: Map[String, String] = System.getenv()

  // keys for unpacking a thrift version
  /**
   * name of the thrift binary
   */
  val thriftName = SettingKey[String]("thrift-name", "name of the thrift binary to use")
  /**
   * the current os/architecture to use for thrift
   */
  val thriftPlatform = SettingKey[String]("thrift-platform", "determines which thrift binary to use. Found using os.name and os.arch from system properties")
  /**
   * task to extract a binary from our plugin jar
   */
  val thriftBin = TaskKey[File]("thrift-bin", "extrat a suitable thrift binary from the plugin jar")
  // keys for generating thrift
  /**
   * directory to look in for thrift sources
   */
  val thriftSourceDir = SettingKey[File]("thrift-sources-dir", "thrift source dir")
  /**
   * languages to generate thrift code for.
   */
  val thriftGenLangs = SettingKey[Seq[String]]("thrift-gen-langs", "languages to generate thrift code for")
  /**
   * languages to compile thrift code for.
   */
  val thriftCompileLangs = SettingKey[Seq[String]]("thrift-compile-langs", "languages to compile thrift code for. This is passed to sbt for further compile")
  /**
   * folders to look in for include thrift files
   */
  val thriftIncludeFolders = SettingKey[Seq[File]]("thrift-include-folders", "folders to use in thrift includes")
  /**
   * a set of thrift source files
   */
  val thriftSources = SettingKey[Seq[File]]("thrift-sources", "thrift sources")
  /**
   * where to spit out generated thrift. Note that thrift itself will add "java",
   * "rb", etc. to the end of this
   */
  val thriftOutputDir = SettingKey[File]("thrift-output-dir", "thrift sources")
  /**
   * do we need to generate stuff
   */
  val thriftIsDirty = TaskKey[Boolean]("thrift-is-dirty", "do we need to regenerate")
  /**
   * the actual task to generate thrift
   */
  val thriftGen = TaskKey[Seq[File]]("thrift-gen", "generate code from thrift files")

  val genThriftSettings: Seq[Setting[_]] = Seq(
    thriftSourceDir <<= (sourceDirectory) { _ / "thrift" },
    thriftGenLangs := Seq("java"),
    thriftCompileLangs <<= thriftGenLangs,
    thriftSources <<= (thriftSourceDir) { srcDir => (srcDir ** "*.thrift").get },
    thriftOutputDir <<= (sourceManaged).identity,
    thriftIncludeFolders := Seq(),
    // look at includes and our sources to see if anything is newer than any of our output files
    thriftIsDirty <<= (streams,
                       thriftSources,
                       thriftOutputDir,
                       thriftIncludeFolders) map { (out, sources, outputDir, inc) => {
      // figure out if we need to actually rebuild, based on mtimes
      val allSourceDeps = sources ++ inc.foldLeft(Seq[File]()) { (files, dir) => files ++ (dir ** "*.thrift").get }
      val sourcesLastModified:Seq[Long] = allSourceDeps.map(_.lastModified)
      val newestSource = if (sourcesLastModified.size > 0) {
        sourcesLastModified.max
      } else {
        Long.MaxValue
      }
      val outputsLastModified = (outputDir ** "*.scala").get.map(_.lastModified)
      val oldestOutput = if (outputsLastModified.size > 0) {
        outputsLastModified.min
      } else {
        Long.MinValue
      }
      oldestOutput < newestSource
    }},
    // actually run thrift
    thriftGen <<= (streams,
                   thriftIsDirty,
                   thriftGenLangs,
                   thriftCompileLangs,
                   thriftSources,
                   thriftOutputDir,
                   thriftBin,
                   thriftIncludeFolders) map { (out, isDirty, langs, compLangs, sources, outputDir, bin, inc) =>
      out.log.info("generating thrift for %s...".format(sources.mkString(", ")))
      outputDir.mkdirs()
      if (isDirty) {
        val sourcePaths = sources.mkString(" ")
        val thriftIncludes = inc.map { folder =>
          "-I " + folder.getAbsolutePath
        }.mkString(" ")
        langs.map { lang =>
          sources.foreach { path =>
            val cmd = "%s %s --gen %s -o %s %s".format(
              bin, thriftIncludes, lang, outputDir.getAbsolutePath, path)
            out.log.info(cmd)
            <x>{cmd}</x> !
          }
        }
      }
      compLangs.flatMap( lang => ((outputDir / ("gen-" + lang)) ** "*.%s".format(lang)).get.toSeq)
    },
    sourceGenerators <+= thriftGen
  )

  val newSettings: Seq[Setting[_]] = Seq(
    thriftName := "thrift",
    thriftPlatform := {
      System.getProperty("os.name") match {
        case "Mac OS X" => "osx10.6"
        case "Linux" => System.getProperty("os.arch") match {
          case "i386" => "linux32"
          case "amd64" => "linux64"
          case arch => throw new Exception(
            "No thrift linux binary for %s, talk to william@twitter.com".format(arch))
        }
        case "FreeBSD" => System.getProperty("os.arch") match {
          case "amd64" => "bsd64"
          case arch => throw new Exception(
            "No thrift BSD binary for %s, talk to brandon@twitter.com".format(arch))
        }
        case unknown => throw new Exception(
          "No thrift binary for %s, talk to marius@twitter.com".format(unknown))
      }
    },
    thriftBin <<= (thriftPlatform, thriftName) map { (platform, name) =>
      environment.get("SBT_THRIFT_BIN") map(new File(_)) getOrElse {
        CompileThrift.synchronized {
          val cached = for {
            (cachedThrift, cachedPath) <- cachedThriftPath
            if cachedThrift == name
          } yield cachedPath
          cached getOrElse {
            val binName = "%s.%s".format(name, platform)
            val stream = getClass.getResourceAsStream("/thrift/%s".format(binName))
            val file = File.createTempFile(name, "")
            IO.transfer(stream, file)

            <x>chmod 0500 {file.getAbsolutePath}</x> !

            cachedThriftPath = Some((name, file))
            file
          }
        }
      }
    }
  ) ++ inConfig(Test)(genThriftSettings) ++ inConfig(Compile)(genThriftSettings)
}
