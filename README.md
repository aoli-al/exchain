# ExChain: Efficient Dynamic Exception Tracing


ExChain is a static/dynamic analysis tool aimed at pinpointing the root cause of failures resulting from exception propagation.

# Build

- To build ExChain and benchmark applications you need to download all dependencies.

```
git submodule update --init --recursive
```

- ExChain uses JVMTi to monitor exceptions in the application. To build the JVMTi plugin:


```
./gradlew :native:build
```

- ExChain uses [Phosphor](https://github.com/gmu-swe/phosphor) to perform dynamic taint tracking.





# Dependencies

- To build JVMTI plugin `./gradlew :native:build`
- To build javaagent `./gradlew shadowJar`

# Run

## Prerequest

You need to download and compile phosphor before running the application.
Please follow the document from phosphor.

## Demo

We provide a demo application in `demo` folder to show the basic functionalities.
You can run demo application with ExChain with command
`./gradlew :demo:runInstrumentedJar`

## Fineract

- Download and compile the source code from github https://github.com/apache/fineract.
- Prepare the instrumented JDK (follow the instructions from phosphor)
- run the application with javaagents and agentpath (check
`:demo:runInstrumentedJar` task to understand how to set those variables).
