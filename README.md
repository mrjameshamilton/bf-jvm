# Brainf*ck JVM Compiler

A compiler that compiles Brainf*ck input to Java bytecode that can be executed on a
JVM. 

The compiler is simple and does no optimizations.
For an optimizing compiler [see here](https://github.com/mrjameshamilton/bf).

# Run

Compile a Brainf*ck program by passing the input file path and the output jar path
to the compiler:

```shell
$ ./gradlew run --args "examples/hellojvm.bf build/output.jar"
```

The compiled jar can then be executed:

```shell
$ java -jar build/output.jar
```
