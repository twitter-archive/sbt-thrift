import com.twitter.sbt._

version := "0.1"

scalaVersion := "2.9.1"

resolvers += "twitter" at "http://maven.twttr.com/"

seq(CompileThrift.newSettings:_*)

libraryDependencies ++= Seq("thrift" % "libthrift" % "0.5.0",
                    "org.slf4j" % "slf4j-nop" % "1.6.2")