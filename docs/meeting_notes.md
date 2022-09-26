---
title: Meeting Notes
---

# Sep 18

## Progress

- Goal: Enabling taint propagation dynamically
- Idea: add if branch in each method


```{.java .numberLines .lineAnchors}
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


- Implementation is done.
- We instrument Fineract and tested the implementation using integration tests.
-  All tests are passed (with/without taint propagation).
    - Overhead measurement is pending.
- Enable taint propagation in the middle of program execution makes the application crash!


## Examples

Origin Program:

```{.java .numberLines .lineAnchors}
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

```{.java .numberLines .lineAnchors}
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

```{.java .numberLines .lineAnchors}
void run() {
    tracingEnabled = false;
    Foo foo = new Foo();
    tracingEnabled = true;
    foo.test();
}
```
## Ideas

- What if we only taint objects
    - Performance impact is low
    - We cannot handle:
        - local null pointers
        - local variables with primitive types


Origin:
```{.java .numberLines .lineAnchors}
public class Foo {
    void localVars() {
        int a = 0;
        Object b = null;
        Foo c = new Foo();
    }
}
```

Fully Instrumented:
```{.java .numberLines .lineAnchors}
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
```{.java .numberLines .lineAnchors}
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

```{.java .numberLines .lineAnchors}
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
```{.java .numberLines .lineAnchors}
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

```{.java .numberLines .lineAnchors}
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
- Identify branch condition: `id != totalCommits+1`
- totalCommits is passed as a local variable and the taint is missing.


# Sep 26


## Revisit

Identify the causal relationships among exceptions.

- Input: source code/bytecode of the system
- Output: Causality chain of system internal exceptions

## Related Works

- Use logs to disambiguate call paths of executions. [[OSDI 14](http://log20.dsrg.utoronto.ca/log20_sosp17_paper.pdf),
[ATC 18](https://www.usenix.org/system/files/conference/atc18/atc18-luo.pdf), [MICRO-96](https://web.eecs.umich.edu/~weimerw/2018-481/readings/pathprofile.pdf)]
    - Focus on execution trace reconstruction when failure happens.
    - Different output: Execution traces are not sufficient to debug the root cause
    of the system when the exceptions are thrown across requests.
- Find logs that are related to root cause of failures. [[MLSys 22](http://web.cs.ucla.edu/~dogga/publications/mlsys22.pdf)]
    - Use machine learning to generate __queries__ for finding root causes
    in distributed systems.
    - Different output: User may not log sufficient data
    to help developers to identify
        - Counter point: what if we log all exceptions automatically, can we use such techniques to identify all exceptions that are related to the failure?
        - Such tool cannot tell the causality among exceptions.

- Distributed tracing. [[NSDI 07](https://www.usenix.org/conference/nsdi-07/x-trace-pervasive-network-tracing-framework), [OSDI 13](https://dl.acm.org/doi/10.1145/2815400.2815415)]
    - Different problem: try to construct the causal paths
    in network protocols.
    - They complement each other.

- Monitoring. [[NSDI 20](https://www.cs.jhu.edu/~huang/paper/omegagen-nsdi20-preprint.pdf)]
    - Monitor all sensitive API calls in applications.
    - High overhead.
    - Do not show the causality among exceptions.




## High Level Design


Given an exception $e$ we want to compute:

- source variables $S_e$: a set of variables that cause
the exception $e$.
- affected variables $A_e$: a set variables whose values are affected
by the exception $e$.
- The propagation of $A_e$: how affected variables affect the state
of the program.

We define:
- Exception $e_1$ is caused by exception $e_2$ if and only if
the intersection of $S_{e_1}$ and $A_{e_2}$ is not empty.


Example:


```{.java .numberLines .lineAnchors}
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

- RuntimeException:16:
    - source var: `this` or none
    - affected var: `cert`
- NullPointerException:10
    - source var: `cert`
    - affected var: `client`
- RequestException:20
    - source var: `client`, `r`
    - affected var: `client`


## Compute Source Variables $S_e$

Algorithm:

- If the exception is NPE
    - Callee is the source variable
- If the exception is OutOfBoundException
    - Callee and arguments are the source variables
- If the exception is an AssertError or the exception is
from a throw instruction
    - Identify the closest branch instruction and its
    variables are source variables.
    - This is a heuristic


Example:

```{.java .numberLines .lineAnchors}
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

The source
The source variables of `RuntimeException` are `id` and `totalCommits`.


## Compute Affected Variables $A_e$

Algorithm:

- Given an exception $e$. Let $A_e\leftarrow \emptyset$
- for each function in the stack trace:
    - Run the data flow analysis $DF_1$ to compute the source values $SV_1$
    of each variable without exception path.
    - Run the data flow analysis $DF_2$ to compute the source values $SV_2$
    of each variable with exception path.
    - For instructions that are only executed in $DF_1$ and $DF_2$
        - If the instruction updates a variable $v$ and $\ SV_1[\![v]\!] \not=SV_1[\![v]\!]$:
            - $A_e\leftarrow A_e\cup v$
        - If the instruction calls a method of a variable $v$:
            - $A_e\leftarrow A_e\cup v$
    - If the exception is caught in the current function then break

Example:

Let's only consider the `RuntimeException` thrown by `getCert`
method.

```{.java .numberLines .lineAnchors}
void setup(HTTPClient client, Request r) {
    Cert cert = null;                       // cert = null
    try {
        cert = getCert();                   // cert = invokevirtual getCert
    } catch (Exception e) {
        LOG.error("Cert failure");
    }
}
```

- Without exception line 2, 4 are executed.
- With exception line 2, 6 are executed.
- Affected vars are `cert` and `LOG`.


## Compute the Propagation of $A_e$

### Static Analysis

Idea: Use static data-flow analysis to identify the taint relationships
offline.

Challenges 1: what is the entry point of each program:

- Pattern 1:
    - Exceptions happen in the same thread, same execution
    - Exceptions happen in different threads, different execution

```{.java .numberLines .lineAnchors}
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

```{.java .numberLines .lineAnchors}
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

```{.java .numberLines .lineAnchors}
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

- Lattice: a set of exception labels.
    - Top: all exceptions
    - Bot: empty set


- Pros:
    - The algorithm is performed offline. Overhead is low.
- Cons:
    - False positives


### Dynamic Taint Analysis

Idea: taint all affected variables $A$ with exception ID.

- Pros
    - Available taint analysis framework
- Cons
    - High overhead (> 400%)

