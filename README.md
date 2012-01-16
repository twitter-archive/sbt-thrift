# Overview

Sbt-thrift is an sbt 0.11 plugin that adds a mixin for doing thrift code auto-generation during your

## Building

Until standard-project is released as an sbt 0.11 plugin, you'll need
to publish local. So... 

    $sbt publish-local


## Testing

There is a really crude scripted plugin. You can run it with

    $sbt scripted
    
## How it works

The plugin registers itself as a source generator for the compile
phase. Settings are added for both compile and test phases, so if you
have thrift files in src/test/thrift they'll get processed too.

## Getting SbtThrift

See https://github.com/harrah/xsbt/wiki/Plugins for information on
adding plugins. In general, you'll need to add the following to your
project/plugins.sbt file:

    addSbtPlugin("com.twitter" % "sbt-thrift % "11.0.0-SNAPSHOT")

## Mixing in SbtThrift

### Using a .sbt file

    import com.twitter.sbt._

    seq(SbtThrift.newSettings: _*)
    
    
### Using a .scala build definition

    import sbt._
    import Keys._
    import com.twitter.sbt._
    
    object Util extends Build {
        lazy val root = Project(id = "util", base = file("."))
            settings (SbtThrift.settings: _*)
    }

## Settings Overview

* thrift-name - name of thrift binary to use. Usually "thrift", but
  can be "thrift-finagle" if you want finagle bindings.
* thrift-platform - qualifier for the thrift binary to use calculated
  from os.arch and os.name.
* thrift-bin - the File to use for thrift gen. Usually extracted from
  the jar, but overridable with the SBT__THRIFT__BIN environment
  variable
* thrift-sources-dir - where thrift source files are
* thrift-gen-langs - languages to emit. This is just for thrift gen.
  It doesn't necessarily pass _all_ of these to sbt for compilation.
* thrift-compile-langs - languages to compile. Java/Scala only, unless
  you extend stuff more
* thrift-include-folders - folders to search for thrift includes.
* thrift-sources - thrift files to process. Generated from
  thrift-sources-dir
* thrift-output-dir - where to emit thrift files.
* thrift-is-dirty - should we re-gen thrift files
* thrift-gen - actually generate thrift

## Where did scala thrift gen go?

See https://github.com/twitter/sbt-scrooge

