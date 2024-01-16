![](./figs/ExChain.png)

ExChain is a static/dynamic analysis tool aimed at pinpointing the root cause of failures resulting from exception propagation.


## Tech Detail

Please read our paper: [ExChain: Exception Dependency Analysis for Root Cause Diagnosis](./docs/exchain.pdf)

## Prerequest

Different dependencies/applications use different versions of Java🤯. To smooth the build process,
we use [jenv](https://www.jenv.be/) to provide some hints.  Please make sure you have Java 8, Java 11, and Java >16 installed.


## Build

- To build ExChain and benchmark applications you need to download all dependencies.

```
git submodule update --init --recursive
```

- ExChain uses JVMTi to monitor exceptions in the application. To build the JVMTi plugin:


```
./gradlew :native:build
```

- ExChain uses [Phosphor](https://github.com/gmu-swe/phosphor) to perform dynamic taint tracking and uses [TaintDroid](https://github.com/secure-software-engineering/FlowDroid)
to perform static taint tracking. Unfortunately none of those tools work smoothly for large applications such as Hadoop 🥲.
We have to fork them and add patches.

- Our build script will configure and build them for you:


```
./gradlew :native:createShadow
```

## Benchmark Applications

Now you can use ExChain to analyze failures in applications! We have provided testing harness for 11 failures. They are located in the `applications` folder. Each folder represents a
failure. E.g. folder `fineract-1211` contains the issue [FINERACT-1211](https://issues.apache.org/jira/browse/FINERACT-1211). Note that this folder contains issues that wasn't successfully
reproduced.

To find all reproducable issues, you may look at scripts in `applications/scripts` folder. Files with name `{application}_{issue_id}` represent a harness script that reproduce the failure. You
may look at the script to understand how each application is compiled and executed.

We also provide a script that help you to run those applications with ExChain directly

```
cd applications/scripts
python3 runner.py --help
```

For example, if you are interested in wicket-6908:

```
python3 runner.py wicket_6908 build
python3 runner.py wicket_6908 instrument # only need for full dynamic taint analysis
python3 runner.py wicket_6908 run --type [type]
```

Here with type you can have: origin|static|hybrid|dynamic.

After running, the results are located in the folder `applications/wicket-6908/[type]-results/{datetime}/`. For static you will see `affected-var-results.json`, `exception.json`. For hybrid and dynamic you will see `affected-var-results.json`, `dynamic_dependency.json`, `exception.json`.

For static and hybrid, you may also want to use static taint analysis:

```
python3 runner.py wicket_6908 analyze --type [type]
```

## Results

- `exception.json`: this file contains the basic information of all exceptions thrown during the execution.
- `affected-var-results.json`: this file contains the affected and responsible var analysis results for each exception thrown during the execution.
- `dynamic_dependency.json`: this file contains exception chain analysis results using dynamic/hybrid taint analysis.
- `dependency.json`: this file contains exception chain results using static taint analysis.
