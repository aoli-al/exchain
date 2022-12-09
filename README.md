ExChain: Efficient Dynamic Exception Tracing

# Dependencies

ExChain depends on [Phosphor](https://github.com/gmu-swe/phosphor/tree/phosphor-0.1.0-dev) and plase
use the `phosphor-0.1.0-dev` branch.

Make sure both ExChain and Phosphor are placed in `~/repos` directory.

# Build

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
