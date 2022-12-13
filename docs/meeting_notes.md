---
title: Meeting Notes
---

# Sep 18

## Progress

-   Goal: Enabling taint propagation dynamically
-   Idea: add if branch in each method

``` {.java .numberLines .lineAnchors}
// origin version
public class Foo {
    public foo() {
        // impl
    }
}

// current version
public class Foo {
    public foo() {
        if (enabled) {
            // instrumented impl
        } else {
            // origin impl
        }
    }
}
```

-   Implementation is done.
-   We instrument Fineract and tested the implementation using
    integration tests.
-   All tests are passed (with/without taint propagation).
    -   Overhead measurement is pending.
-   Enable taint propagation in the middle of program execution makes
    the application crash!

## Examples

Origin Program:

``` {.java .numberLines .lineAnchors}
public class Foo {
    Object a;
    public Foo() {
        a = new int[3];
    }
    void test() {
        ((int[]) a)[0] = 5;
    }
}
```

Instrumented Program:

``` {.java .numberLines .lineAnchors}
public class Foo {
    Object a;
    Taint a_taint;
    public Foo() {
        a = new TaggedArray(new int[3]);
    }
    void test() {
        ((TaggedArray) a).put(0, 5);
    }
}
```

Problem:

``` {.java .numberLines .lineAnchors}
void run() {
    tracingEnabled = false;
    Foo foo = new Foo();
    tracingEnabled = true;
    foo.test();
}
```

## Ideas

-   What if we only taint objects
    -   Performance impact is low
    -   We cannot handle:
        -   local null pointers
        -   local variables with primitive types

Origin:

``` {.java .numberLines .lineAnchors}
public class Foo {
    void localVars() {
        int a = 0;
        Object b = null;
        Foo c = new Foo();
    }
}
```

Fully Instrumented:

``` {.java .numberLines .lineAnchors}
public class Foo {
    Taint thisTag;
    void localVars() {
        int a = 0;
        Taint a_tag;
        Object b = null;
        Taint b_tag;
        Foo c = new Foo();
        Taint c_tag; // taint the reference.
        c.thisTag = "some tag"; // taint the heap object.
    }
}
```

Partially Instrumented:

``` {.java .numberLines .lineAnchors}
public class Foo {
    Taint thisTag;
    void localVars() {
        int a = 0;
        Object b = null;
        Foo c = new Foo();
        c.thisTag = "some tag";
    }
}
```

Case Study:

Can handle partially:

``` {.java .numberLines .lineAnchors}
void setup(HTTPClient client, Request r) {
    Cert cert = null;
    try {
        cert = getCert(); // throws RuntimeException
        // We cannot taint Cert object because it is null.
    } catch (Exception e) {
        LOG.error("Cert failure");
    }
    // Other initialization logic
    try {
        client.setCert(cert.getData()); // throws NPE
        // We can taint Client object here
        // client.setTaint("NPE")
    } catch (Exception e) {
        LOG.error("client failure");
    }
}
void sendRequest(HTTPClient client, Request r) {
    setup(client);
    client.sentRequest(r); // throws RequestException
    // client.getTaint()
}
```

Cannot handle:

``` {.java .numberLines .lineAnchors}
void sendRequest(Request r) {
    int retry = 0;
    while (retry < MAX_RETRY) {
        try {
            throw new Exception1();
            break;
        }
        catch (Exception e) {
            retry++;
            LOG.error(e);
        }
    }
    if (retry >= MAX_RETRY) {
        throw new TooManyRetryException();
    }
}
```

More complicated:

``` {.java .numberLines .lineAnchors}
class Loader {
    int totalCommits = 0;
    void recoverRequest(Checkpoint checkpoints) {
        try {
            for (commit: checkpoints) {
                if (condition) {
                    throw new OOMException();
                }
                totalCommits++;
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    void commitRequest(int id) {
        checkId(id, totalCommits);
        //...
        totalCommits++;
    }

    void checkId(int id, int totalCommits) {
        if (id != totalCommits+1) {
            throw new InconsistentIdException();
        }
    }
}
```

Two challenges:

-   Identify branch condition: `id != totalCommits+1`
-   totalCommits is passed as a local variable and the taint is missing.

# Sep 26

## Revisit

Identify the causal relationships among exceptions.

-   Input: source code/bytecode of the system
-   Output: Causality chain of system internal exceptions

## Related Works

-   Use logs to disambiguate call paths of executions. \[[OSDI
    14](http://log20.dsrg.utoronto.ca/log20_sosp17_paper.pdf), [ATC
    18](https://www.usenix.org/system/files/conference/atc18/atc18-luo.pdf),
    [MICRO-96](https://web.eecs.umich.edu/~weimerw/2018-481/readings/pathprofile.pdf)\]

    -   Focus on execution trace reconstruction when failure happens.
    -   Different output: Execution traces are not sufficient to debug
        the root cause of the system when the exceptions are thrown
        across requests.

-   Find logs that are related to root cause of failures. \[[MLSys
    22](http://web.cs.ucla.edu/~dogga/publications/mlsys22.pdf)\]

    -   Use machine learning to generate **queries** for finding root
        causes in distributed systems.
    -   Different output: User may not log sufficient data to help
        developers to identify
        -   Counter point: what if we log all exceptions automatically,
            can we use such techniques to identify all exceptions that
            are related to the failure?
        -   Such tool cannot tell the causality among exceptions.

-   Distributed tracing. \[[NSDI
    07](https://www.usenix.org/conference/nsdi-07/x-trace-pervasive-network-tracing-framework),
    [OSDI 13](https://dl.acm.org/doi/10.1145/2815400.2815415)\]

    -   Different problem: try to construct the causal paths in network
        protocols.
    -   They complement each other.

-   Monitoring. \[[NSDI
    20](https://www.cs.jhu.edu/~huang/paper/omegagen-nsdi20-preprint.pdf)\]

    -   Monitor all sensitive API calls in applications.
    -   High overhead.
    -   Do not show the causality among exceptions.

-   https://valgrind.org/docs/origin-tracking2007.pdf

## High Level Design

Given an exception $e$ we want to compute:

-   source variables $S_e$: a set of variables that cause the exception
    $e$.
-   affected variables $A_e$: a set variables whose values are affected
    by the exception $e$.
-   The propagation of $A_e$: how affected variables affect the state of
    the program.

We define:

-   Exception $e_1$ is caused by exception $e_2$ if and only if the
    intersection of $S_{e_1}$ and $A_{e_2}$ is not empty.

Example:

``` {.java .numberLines .lineAnchors}
void setup(HTTPClient client, Request r) {
    Cert cert = null;
    try {
        cert = getCert();
    } catch (Exception e) {
        LOG.error("Cert failure");
    }
    // Other initialization logic
    try {
        client.setCert(cert.getData()); // throws NPE
    } catch (Exception e) {
        LOG.error("client failure");
    }
}
Cert getCert() {
    throw new RuntimeException();
}
void sendRequest(HTTPClient client, Request r) {
    setup(client);
    client.sentRequest(r); // throws RequestException
}
```

-   RuntimeException:16:
    -   source var: `this` or none
    -   affected var: `cert`
-   NullPointerException:10
    -   source var: `cert`
    -   affected var: `client`
-   RequestException:20
    -   source var: `client`, `r`
    -   affected var: `client`

## Compute Source Variables $S_e$

Algorithm:

-   If the exception is NPE
    -   Callee is the source variable
-   If the exception is OutOfBoundException
    -   Callee and arguments are the source variables
-   If the exception is an AssertError or the exception is from a throw
    instruction
    -   Identify the closest branch instruction and its variables are
        source variables.
    -   This is a heuristic

Example:

``` {.java .numberLines .lineAnchors}
class Loader {
    int totalCommits = 0;

    void commitRequest(int id) {
        checkId(id, totalCommits);
        //...
        totalCommits++;
    }

    void checkId(int id, int totalCommits) {
        if (id != totalCommits+1) {
            logAndThrowException("Inconsistent ID");
        }
    }

    void logAndThrowException(String message) {
        Log.error(message);
        throw new RuntimeException();

    }
}
```

The source The source variables of `RuntimeException` are `id` and
`totalCommits`.

## Compute Affected Variables $A_e$

Algorithm:

-   Given an exception $e$. Let $A_e\leftarrow \emptyset$
-   for each function in the stack trace:
    -   Run the data flow analysis $DF_1$ to compute the source values
        $SV_1$ of each variable without exception path.
    -   Run the data flow analysis $DF_2$ to compute the source values
        $SV_2$ of each variable with exception path.
    -   For instructions that are only executed in $DF_1$ and $DF_2$
        -   If the instruction updates a variable $v$ and
            $\ SV_1[\![v]\!] \not=SV_1[\![v]\!]$:
            -   $A_e\leftarrow A_e\cup v$
        -   If the instruction calls a method of a variable $v$:
            -   $A_e\leftarrow A_e\cup v$
    -   If the exception is caught in the current function then break

Example:

Let's only consider the `RuntimeException` thrown by `getCert` method.

``` {.java .numberLines .lineAnchors}
void setup(HTTPClient client, Request r) {
    Cert cert = null;                       // cert = null
    try {
        cert = getCert();                   // cert = invokevirtual getCert
    } catch (Exception e) {
        LOG.error("Cert failure");
    }
}
```

-   Without exception line 2, 4 are executed.
-   With exception line 2, 6 are executed.
-   Affected vars are `cert` and `LOG`.

## Compute the Propagation of $A_e$

### Static Analysis

Idea: Use static data-flow analysis to identify the taint relationships
offline.

Challenges 1: what is the entry point of each program:

-   Pattern 1:
    -   Exceptions happen in the same thread, same execution
    -   Exceptions happen in different threads, different execution

``` {.java .numberLines .lineAnchors}
class Loader {
    int totalCommits = 0;
    // Entry point 1
    void recoverRequest(Checkpoint checkpoints) {
        try {
            for (commit: checkpoints) {
                if (condition) {
                    throw new OOMException();
                }
                totalCommits++;
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    // Entry point 2
    void commitRequest(int id) {
        checkId(id, totalCommits);
        //...
        totalCommits++;
    }
    void checkId(int id, int totalCommits) {
        if (id != totalCommits+1) {
            throw new InconsistentIdException();
        }
    }
}
```

Challenge 2: how to model collections?

``` {.java .numberLines .lineAnchors}
void tes1(Object[] a) {
    try {
        throw new Exception();
        a[1] = null; // a[1] is affected var
    } catch (Exception e) {
    }

    a[2].callMethod(); // throws NPE. a[2] is source var.
}
```

Other implementation level challenges:

Challenge 3: how to model function calls?

``` {.java .numberLines .lineAnchors}
void foo(Object a) {
    if (condition) {
        foo(a);
    }
    // a is tainted here
    Object b = a;
    // ...
}
```

Algorithm

-   Lattice: a set of exception labels.

    -   Top: all exceptions
    -   Bot: empty set

-   Pros:

    -   The algorithm is performed offline. Overhead is low.

-   Cons:

    -   False positives

### Dynamic Taint Analysis

Idea: taint all affected variables $A$ with exception ID.

-   Pros
    -   Available taint analysis framework
-   Cons
    -   High overhead (\> 400%)

# Oct 2

-   Analyzed Hadoop HDFS-4128

    -   SecondaryNameNode is a background service that runs periodically
    -   By default, it runs the checkout method every \~30min.
    -   There is an execution where an exception makes the service into
        bad stats.
    -   In the following execution the service crashes.

-   Simulation environment:

    -   We force the SecondaryNameNode to run two consecutive checkouts.
    -   In the first execution we inject the error.
    -   In the second execution we observe the crash.

-   Raw logging:

```{=html}
<!-- -->
```
    ClassNotFoundException 11041
    FileNotFoundException 17
    IOException 24
    UnsatisfiedLinkError 1
    NoSuchMethodException 12
    NoSuchMethodError 2
    MalformedURLException 4
    ConfigurationException 1
    NoSuchFieldException 1
    NotCompliantMBeanException 18
    UnixException 327
    NoSuchFileException 133
    RuntimeException 5
    XMLEntityScanner$1 4
    UnsupportedOperationException 1
    InvocationTargetException 1
    SecurityException 2
    UnknownHostException 1
    BlockPlacementPolicy$NotEnoughReplicasException 2
    MissingResourceException 1
    EOFException 1
    InterruptedException 198
    AsynchronousCloseException 20

-   We apply the following filters:
    -   discard exceptions that are caught inside JDK
    -   discard exceptions that does not affect the state of the program
        -   Exception does not affect the value of any local variables
        -   Exception does not affect the value of global variables


```{=html}
<!-- -->
```
    java.io.FileNotFoundException 8
    java.lang.ClassNotFoundException 2
    java.io.IOException 10
    java.lang.NoSuchMethodException 5
    java.net.MalformedURLException 2
    org.apache.commons.configuration2.ex.ConfigurationException 1
    java.nio.file.NoSuchFileException 5
    java.lang.RuntimeException 5
    java.lang.UnsupportedOperationException 1
    java.lang.reflect.InvocationTargetException 1
    java.lang.SecurityException 2
    org.apache.hadoop.hdfs.server.blockmanagement.BlockPlacementPolicy$NotEnoughReplicasException 2
    java.lang.InterruptedException 14
    Total: 58

-   For each exception that affects the state of the program:

    -   Affected local objects: 1.96
    -   Affected local arrays 0.02
    -   Affected local primitives: 0.14
    -   Affected local objects that are null: 0.38
    -   Affected class fields: 1.14

-   Most affected variables are objects and fields

    -   Static analysis are bad at tracking global information (class
        fields).
    -   But they are really good at tracking local information (local
        variables).

-   The number of affected primitives and nulls are small

    -   But are they important?
    -   Maybe worth to go to examples.

-   During the exception collection:

    -   It is hard to distinguish which exception belongs to which
        request/execution

TODOS:

-   We still don't know how affected variables propagates in the system
-   Implement static data-flow analysis

# Oct 10

-   We have finished implementing the static data flow analysis

    -   Identify and taint affected global variables ✅
    -   Identify and taint affected local variables 🚧
    -   Propagate taint variables through local variables ✅
    -   Propagate taint variables through global variables ✅

-   Taint Propagation Algorithm:

    -   $G$ a global context that saves the tags for method parameters.
    -   $R$ a global context that saves the tags for method returns.
    -   $D$ a dependency graph that saves all callers of a method $m$
    -   Input: A method $m_{in}$ that throws an exception $e$, a set of
        affected variables $V$,
    -   Initialize the work list $W\leftarrow \{m_{in}\}$
    -   If $W\not=\emptyset$
        -   Remove an element $m$ from $W$
        -   For statement $s$ in $m$
            -   case $s = v_1 := v_2$
                -   If $v_2$ is parameter:
                    -   $tag_{v_1} \leftarrow G[v_2]$
                -   Else
                    -   $tag_{v_1} \leftarrow tag_{v_2}$
                -   If $v_1 \in V$:
                    -   $tag_{v_1} \leftarrow tag_{v_1} \cup \{e\}$
            -   case $s = v := f(v_1)$
                -   If $G[v_1] \cup tag_{v_1} \not= G[v_1]$:
                    -   $G[v_1] \leftarrow G[v_1] \cup tag_{v_1}$
                    -   $W\leftarrow W\cup \{f\}$
                -   $tag_v \leftarrow R[f]$
            -   case $s = \mathrm{return}\ v$
                -   If $R[m] \cup tag_{v} \not= R[m]$:
                    -   $R[m] \leftarrow R[m] \cup tag_{v}$
                    -   for $m'\in D[m]$
                        -   $W\leftarrow W\cup \{m'\}$
    -   Issues:
        -   We assume methods are pure functional (we ignore global
            variables, and side effects of function parameters)

-   Implementation challenges:

    -   Mapping between static information and dynamic information
        -   Bytecode offset v.s. line number
            -   Runtime: bytecode offset (which instruction throws the
                exception)
            -   Static: Soot does not preserve bytecode offset.
                -   https://www.sable.mcgill.ca/soot/tutorial/usage/
            -   Solution
                -   not sure if it is because I'm using Jimple
                    representation. I'll try to use shimple to see if it
                    works
                -   Use ASM and debug information to construct the
                    mapping between bytecode offset and line numbers.
        -   Stack index v.s. variable name

# Oct 17

-   We use Soot InfoFlow to conduct static taint analysis.
    -   Context-insensitive
    -   Aggressive time and memory constraints
-   We successfully analyzed HDFS (HDFS-4128) and Fineract (FINERACT-1211)
    -   Fineract:
        -   Still running
        -   117 false positives
            -   4 after deduplicate
        -   4888 exceptions thrown
        -   3644 exceptions affects the state of the program
        -   If the exception affects the state of the program, on
            average, each exception causes 8 affected local variable, 5
            affected class fields
    -   HDFS:
        -   139 exceptions thrown
        -   125 exceptions affects the state of the program
        -   If the exception affects the state of the program, on
            average, each exception causes 5 affected local variable, 2
            affected class fields
        -   0 false positive reported!

## False Positives

-   There are two sources of false positives:
-   Source 1: the propagation happens, but the propagation is not the
    root cause of the exceptions
    -   Inaccurate affected/source var identification
    -   Solution: collect more false positives, label them based on
        domain knowledge

``` {.java .numberLines .lineAnchors}
void firstException() {
    //...
    if (error) {
        throw new RuntimeException();
    }
    //...
    logger.info("Finished"); // logger is identified as affected field
}

void secondException(String info) {
    if (logger != null) {  // logger is identified as source field
        logger.error(info);
        throw new RuntimeException(info);
    }
}
```

Note: I didn't reproduce the exception manually so it is unclear if the
`logger` object in two methods points to the same heap object.

-   Source 2: the propagation does not happen.
    -   Inaccurate static taint analysis
    -   Candidate solution: can we prune out those impossible

``` {.java .numberLines .lineAnchors}
void process()
  {
    int i;
    if ((i = inputStream.read(nextCharBuf, maxNextCharInd,
                                        4096 - maxNextCharInd)) == -1) { // maxNextCharInd is source Var
        throw new java.io.IOException();
    }
    else
        maxNextCharInd += i; // maxNextCharInd is affected field
    return;
  }

void read() {
    while (true) {
        process();
    }
}
```

## Oct 24/31


-   We successfully analyzed HDFS (HDFS-4128) and Fineract (FINERACT-1211)
    -   Fineract:
        -   117 false positives
            -  4 after deduplicate
        -   5294 exceptions thrown
        -   3644 exceptions affects the state of the program
        -   If the exception affects the state of the program, on
            average, each exception causes 8 affected local variable, 5
            affected class fields
    -   HDFS:
        -   139 exceptions thrown
        -   125 exceptions affects the state of the program
        -   If the exception affects the state of the program, on
            average, each exception causes 5 affected local variable, 2
            affected class fields
        -   0 false positive reported!


## False Positives

-   the propagation does not happen.
    -   Inaccurate static taint analysis

``` {.java .numberLines .lineAnchors}
class Reader {
    maxNextCharInd = 0;
    void process()
    {
        int i;
        // When IOException 2 is thrown
        // maxNextCharInd is source Var
        if (maxNextCharInd > CONSTANT_LENGTH) {
            throw new java.io.IOException();
        }
        else {
            // When IO Exception 1 is thrown,
            // maxNextCharInd is affected field
            maxNextCharInd += i;
        }
        return;
    }

    void read() {
        while (true) {
            process();
        }
    }
}
void run() {
    Reader r1 = new Reader();
    try {
        r1.read(); // IOException 1 is thrown
    } catch (Exception e) {
    }
    Reader r2 = new Reader();
    try {
        r2.read(); // IOException 2 is thrown
    } catch (Exception e) {
    }
}

```

## Stats

- Out of 5294 thrown exceptions
    - 1464 affect only local variables
    - 63 affect only class fields
    - 2117 affect both local variables and class fields.

## Revisit

- End result is from static analysis. Use dynamic analysis to remove false positives.
    - Main challenge: false positives
    - Idea: use dynamic analysis to collect information to reduce false positives.
    - Issues:
        - False positives are unknown unknowns. Theoretically, the static analysis can never avoid false positives.

- End result is from dynamic analysis. Use static analysis to improve performance.
    - Main challenge: overhead
    - Idea: only enable taint analysis when it is required
        - Temporal
            - dynamically enable and disable taint analysis.
                1. need to understand when to enable/disable taint analysis
                2. implement functionalities to disable/enable taint analysis dynamically.
        - Spacial
            - Only enable dynamic taint analysis for __required__ libraries.
            - Program slicing?


## Temporal

- Issue 1: current implementation changes types of objects dynamically. Enabling and disabling instrumentation dynamically causes type mismatch.
    - Re-implement array taint functionality. Instead of rewrite the type of the original array object. We can store all array wrappers in a centralized location and fetch them every time an array is accessed.

Origin Program:

``` {.java .numberLines .lineAnchors}
public class Foo {
    Object a;
    public Foo() {
        a = new int[3];
    }
    void test() {
        ((int[]) a)[0] = 5;
    }
}
```

Instrumented Program:

``` {.java .numberLines .lineAnchors}
public class Foo {
    Object a;
    public Foo() {
        a = new TaggedArray(new int[3]);
    }
    void test() {
        ((TaggedArray) a).put(0, 5);
    }
}
```

Problem:

``` {.java .numberLines .lineAnchors}
void run() {
    tracingEnabled = false;
    Foo foo = new Foo(); // foo.a is int[]
    // Exception thrown !!
    tracingEnabled = true;
    foo.test();          // foo.a is used as TaggedArray
}
```

- Issue 2: we cannot switch between origin and instrumentation inside a method.
    - When an exception is thrown in `setupOrigin`. We cannot taint the `cert` object immediately because `certTaint` local variable is not available.

``` {.java .numberLines .lineAnchors}
void setup(HTTPClient client) {
    if (GlobalConfig.enabled) {
        setupInstrumented(client);
    } else {
        setupOrigin(client);
    }
}

void setupOrigin(HTTPClient client) {
    Cert cert = null;
    try {
        cert = getCert(); // throws RuntimeException
    } catch (Exception e) {
        // ...
    }
    try {
        client.setCert(cert.getData()); // throws NPE
    } catch (Exception e) {
        //...
    }
}

void setupInstrumented(HTTPClient client) {
    Taint clientTaint = ShadowStack.getArgTaint(0);
    Cert cert = null;
    Taint certTaint = null;
    try {
        cert = getCert(); // throws RuntimeException
        certTaint = ShadowStack.returnTaint;
    } catch (Exception e) {
        // ...
    }
    try {
        var tmp = cert.getData(); // throws NPE
        var tmpTaint = ShadowStack.returnTaint;
        ShadowStack.setArgTaint(0, tmpTaint);
        client.setCert(tmp);
    } catch (Exception e) {
        //...
    }
}
```

## Spacial

- Issue 1: common data structures are used everywhere!
    - Map
    - List
    - Set

# Nov 21


- https://issues.apache.org/
    - Filter used
        - Type: bug
        - Resolution: Fixed, Done, Implemented
        - Keyword: exception handling
        - Tag: Resolved, Closed
    - Ordered by created
    - Manually checked top 500 issues
        - Only focus on Java projects
        - Ignore issues without patches
        - Ignore duplicate issues
- We identified 22 issues related to chain of exception handling
    - From 16 different applications
    - > Not sure what causes the NPS because the exception is swallowed.
    - > In rare case, some exceptions may happen.
    - > As a result the original exception is swallowed and the pool never recovers from this state.
    - The final exception of  9 / 21 issues are NPE.
    <!-- - our proposed solution can handle 18/21 issues. (this is based on manually inspection) -->

## Issue: data flow between heap objects and local variables

- 3/22 issues.
- We cannot track the source of a null pointer if it is passed from a field to a local variable.

- We may want to track different types of variables differently.
- If the affected variables are primitive values, null pointers, we use static data-flow analysis
    - The sink and source in static data-flow analysis are both primitive values or null values.
- If the affected variables are heap objects, class fields, we taint the heap object dynamically.


```{.java .numberLines .lineAnchors}
class Foo {
    String s = null;
    Taint s_taint = emptyTaint();
}
void test() {
    Foo f = new Foo();
    try {
        f.s = getWithException(); // throw RuntimeException();
        // f.s is affected variable.
        // s belongs to a heap object.
        // f.s_taint = "RuntimeException()";
    } catch (Exception e) {
    }
    String s = f.s; // The taint information is lost here.
    if (s.IsEmpty()) { // Throws NPE
    // s is a local variable points to null
    // We set s as the sink in the static taint analysis.
    // We failed to track the source of null because the taint
    // information is only maintained dynamically.
    }
}
```

- What we can handle

```{.java .numberLines .lineAnchors}
class Foo {
    String s = null;
    Taint s_taint = emptyTaint();
}
void test() {
    Foo f = new Foo();
    try {
        f.s = getWithException(); // throw RuntimeException();
        // f.s is affected variable.
        // s belongs to a heap object.
        // f.s_taint = "RuntimeException()";
    } catch (Exception e) {
    }
    if (f.s.IsEmpty()) { // Throws NPE
    // s is a field of a heap object
    // we can get taint information of s from f.s_taint
    }
}
```


```{.java .numberLines .lineAnchors}
void test() {
    String s = null;
    try {
        s = getWithException(); // throw RuntimeException();
        // s is affected variable.
        // s is a local variable that points to null.
        // we use static taint analysis to propagate s.
    } catch (Exception e) {
    }
    String a = s;
    if (a.IsEmpty()) { // Throws NPE
    // a is a local variable points to null
    }
}
```

## Issue: control flow dependencies

- 2/22 issues

```{.java .numberLines .lineAnchors}
try {
    throw Exception1();
} catch (Exception e) {
    throw Exception2();
}
```

## Issue: root cause is failure requests

- 1/22 issues

```{.java .numberLines .lineAnchors}
var response = getResponse(); // 500 response
response.getData(); // BadDataException
```

# Nov 28


- Issue collection:
    - https://issues.apache.org/
        - Filter used
            - Type: bug
            - Resolution: Fixed, Done, Implemented
            - Keyword: exception handling
            - Tag: Resolved, Closed
        - Ordered by created
        - Manually checked top 500 issues
            - Only focus on Java projects
            - Ignore issues without patches
            - Ignore duplicate issues
    - We identified 22 issues related to chain of exception handling from 16 different applications.

- Recap:
    - We may want to track different types of variables differently.
    - If the affected variables are primitive values, null pointers, we use static data-flow analysis
        - The sink and source in static data-flow analysis are both primitive values or null values.
    - If the affected variables are heap objects, class fields, we taint the heap object dynamically.

- Issue:
    - This approach has an unrealistic assumption: there is no data-flow between local variables and class fields.
    - ~5/22 collected issues have this property

For example:

```{.java .numberLines .lineAnchors}
class Foo {
    String s = null;
    Taint s_taint = emptyTaint();
}
void test() {
    Foo f = new Foo();
    try {
        f.s = getWithException(); // throw RuntimeException();
        // f.s is affected variable.
        // s belongs to a heap object.
        // f.s_taint = "RuntimeException()";
    } catch (Exception e) {
    }
    String s = f.s; // The taint information is lost here.
    if (s.IsEmpty()) { // Throws NPE
    // s is a local variable points to null
    // We set s as the sink in the static taint analysis.
    // We failed to track the source of null because the taint
    // information is only maintained dynamically.
    }
}
```

- Rohan's suggestion:
    - Instead of providing a system that combines static and dynamic approaches, we provide two versions of the system.
    - The static version performs a lightweight instrumentation and only collects exception information of the production system.
    - The dynamic version does not perform any instrumentation to the production system.
    - Once a failure occurs, there are two scenarios:
        - If the failure can be reproduced offline: we then use the dynamic version to instrument the system and construct accurate causality chains of exceptions.
        - If the failure cannot be reproduced offline: we then use the static version to construct the causality chains of exceptions based on the exception data collected in the production system.

# Dec 5

- 22 issues collected in total

### How to Reproduce

- 8 / 22 can be reproduced

- If the current version does not contain the fix
    - revert back to the commit of the fix
- revert the fix
- if a payload is provided in the ticket
    - reproduce the failure with the same payload
- else if a unit test is provided
    - reproduce the failure with the unit test



### Reason Can't Reproduce

- Unknown root cause (11/22):
    - no payload is provided
    - no unit test is provided
    - the root failure is unknown
- Unable to compile (1/22)
    - missing native dependencies
- Hard to trigger (2/22)
    - require long execution
    - require unstable connection between services

# Dec 12

- Major task:
    - Complete the story
    - Have a timeline

- Problem:
    - (TOFIX) Stateful failures are hard to debug
        - Failures contains at least two exceptions
        - The first exception leads the system into a bad state
        - The second exception causes failures
        - (TOFIX) program failures
            - crash
            - failures that can be observed by end users
- Input:
    - source code of the production system
    - a trace of exceptions and the final exception causes a program failure
- Output:
    - a chain of exceptions from the trace that may cause the failure
- Insight:
    - (TOFIX) Tracking the state changes of each exception allows us to construct the causality chain among exceptions. The causality chain is useful to identify the root cause of the failure. (Is this an insight? why or why not?)
    - (TOFIX) why is causality chain is useful?
- Solution:
    - Step 1: identify the affected variables of each exception
    - Step 2: identify the responsible variables of each exception
    - Step 3: identify the propagation of affected variables
- Challenge:
    - Tracking the propagation of affected variables dynamically is expensive in production system.
    - Tracking the propagation of affected variables statically is in accurate.
- Naive Solution 1:
    - Production system with full taint analysis
    - Problem:
        - High overhead
- Naive Solution 2:
    - Analyze causality chain using data-flow analysis
    - Problem:
        - Inaccurate
        - Also slow (need to process all exception in the production system)
- (TOFIX) Solution 1:
    - Insight: tracking heap objects dynamically is cheap, tracking local objects statically is accurate
    - Dynamic heap + static local
- (TOFIX) Solution 2:
    - Insight: failures occurs repetitively in the production system
    - When the first failure occurs, replace the system with an instrumented system
- Evaluation
    - (TOFIX) 8 issues from open source projects
    - Reproduce each issue
    - Use naive solutions to analyze each issue.
    - Use proposed solution to analyze each issue.


```
ZooKeeper-2247

ZooKeeper-3157

ZooKeeper-4203

ZooKeeper-3006

HDFS-4233

HDFS-12248

HDFS-12070

HDFS-13039

HBase-18137

HBase-19608

HBase-19876

Kafka-12508

Kafka-9374
```

