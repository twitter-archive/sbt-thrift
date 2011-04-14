Overview
========

Sbt-thrift is an sbt plugin that adds a mixin for doing thrift code auto-generation during your
compile phase. Choose one of these three:

- `CompileThriftFinagle` - create the java bindings with alternative async interfaces for finagle, in `target/gen-java/`
- `CompileThriftJava` - create just the java bindings, in `target/gen-java/`
- `CompileThriftPython` - create just the python bindings, in `target/gen-py/ (or target/gen-py.twisted/)`
- `CompileThriftRuby` - create just the ruby bindings, in `target/gen-ruby/`
- `CompileThriftScala` - do `CompileThriftFinagle` and `CompileThriftRuby`, but also generate scala wrappers and implicit conversions in `target/gen-scala/`

Scala Thrift Generation
-----------------------

`CompileThriftScala` implements a way of generating a Scala-wrapper for a
[Finagle](http://twitter.github.com/finagle) Thrift service by using JRuby to
inspect thrift-generated ruby code.  It is therefore necessary for input thrift
files to specify ruby and java namespace rules.  If the java namespace is not
specified in a `CompileThriftScala` object, it is assumed to be
`_scala-namespace_.thrift`.  In order to specify these namespaces,
project specifications must provide a list of `ThriftNamespace`s, as follows:

    import sbt._
    import com.twitter.sbt._
    
    /**
     * Describes a scala project that relies on two Thrift IDLs, to be compiled to two different namespaces.
     */
    class TaxesProject(info: ProjectInfo)
      extends StandardServiceProject(info)
      with CompileThriftScala
    {
      def finagleVersion = "1.2.5"
      def finagleCore = "com.twitter" % "finagle-core" % finagleVersion
      def finagleThrift = "com.twitter" % "finagle-thrift" % finagleVersion
      def finagleOstrich = "com.twitter" % "finagle-ostrich4" % finagleVersion
      
      def thriftNamespaces =
        new ThriftNamespace("Calculator", "gov.irs.taxes.calculator") // Java namespace inferred to be gov.irs.taxes.calculator.thrift
	:: new ThriftNamespace( "EZFile", "gov.irs.taxes.ezfile.nondefaultthriftnamespace", "gov.irs.taxes.ezfile")
        :: Nil
    }

