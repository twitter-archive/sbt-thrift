
Sbt-thrift is an sbt plugin that adds a mixin for doing thrift code auto-generation during your
compile phase. Choose one of these three:

- `CompileThriftJava` - create just the java bindings, in `target/gen-java/`
- `CompileThriftFinagle` - create the java bindings with alternative async interfaces for finagle,
  in `target/gen-java/`
- `CompileThriftScala` - do `CompileThriftFinagle`, but also generate scala wrappers and implicit
  conversions in `target/gen-scala/`
