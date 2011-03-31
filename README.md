
Sbt-thrift is an sbt plugin that adds a mixin for doing thrift code auto-generation during your
compile phase. Choose one of these three:

- `CompileThriftFinagle` - create the java bindings with alternative async interfaces for finagle, in `target/gen-java/`
- `CompileThriftJava` - create just the java bindings, in `target/gen-java/`
- `CompileThriftPython` - create just the python bindings, in `target/gen-py/ or target/gen-py.twisted/`
- `CompileThriftRuby` - create just the ruby bindings, in `target/gen-ruby/`
- `CompileThriftScala` - do `CompileThriftFinagle` and `CompileThriftRuby`, but also generate scala wrappers and implicit conversions in `target/gen-scala/`
