---
title: Exception Analysis
---


# Classification

- Exception only affects control flow
    - Exceptions are thrown ([example](#control-flow))
        - within the same method
        - across different methods
- Exception affects both control and data flow
    - Affected vars are initialized ([example](#initialization))
        - before the exception
        - after the exception
    - Affected vars are updated ([example](#update))
        - within the same method
        - across different methods
    - Exception is caught ([example](#catch-location))
        - in the current method
        - in other methods

# Examples

## Control Flow

```{.java .numberLines .lineAnchors}
void test1() {
    try {
        possibleThrown();
    } catch (Exception e) {
        anotherPossibleThrown();
    }
}
void test2() {
    try {
        possibleThrown();
    } catch (Exception e) {
        throw new Exception();
    }
}
```

## Initialization

```{.java .numberLines .lineAnchors}
int local1(Object obj) {
    Object b = obj.getB();
    possibleThrown();
    b.set(5);
    Object c = obj.getC(); // variable `c` is not initialized when exception happends.
    c.set(5);
}
```

## Update

```{.java .numberLines .lineAnchors}
class Foo {
    int a;
    void updateA() {
        a = 3;
    }
}
void test() {
    Foo f1, f2;
    possibleThrown();
    f1.updateA(); // f1.a is updated through a method call.
    f2.a = 3;
}
```

## Catch Location

```{.java .numberLines .lineAnchors}
int local1(Object obj) {
    int a = 5;
    possibleThrown();
    a = 10;
    obj.a = a;
}

int local2(Object obj) {
    int a = 3;
    int c = 0;
    try {
        possibleThrown();
        a = 5;
    } catch(Exception e) {
        obj.a = 10;
    }
    c = 5;
    //...
}
```


## Optimization Oppertunities

### Accuracy
### Performance

- Enabling dynamic taint analysis dynamically


## Static Data-flow Analysis

- Exception only affects control flow ([example](#control-flow-exception))
- Exception affects both contorl and data flow
    - Local/global variables
        - Local variables ([example](#local-variables))
            - Exception Caught Location
                - Exception is not caught in the current method
                    - variables with primitive types are not tainted.
                - Exception is caught by the current method
                    - only varaibles in the try/catch blocks are tainted.
            - Declare location
                - Variables declared before the thrown instruction
                    - Handled by default
                - Variables declared after the thrown instruction
                    - Not handled yet (variable `c` in `local1`)
        - Global variables ([example](#global-variables))
            - Exception Caught Location
                - Exception is not caught in the current method
                    - all class members are tainted.
                - Exception is caught by the current method
                    - only class members in the try/catch blocks are tainted.
    - Method call ([example](#method-call)): intra-procedual analysis, treat method
    call as black box.
        - Non-static method call
            - Read-only
                - Treated as read-write method call (false positive)
            - Read-write
        - Static method call
            - Read-only
            - Read-write
                - Threated as read-only method call (false negative)

## Dynamic Taint Analysis

- Exception only affects control flow ([example](#data-flow))
- Exception affects both contorl and data flow ([example](#control-and-data-flow))

# Examples

## Control Flow Exception

```{.java .numberLines .lineAnchors}
void processRequest(Request r) {
    try {
        throw new Exception1();
    } catch (Exception e) {
        publishEvent(new FailureEvent("failure"));
    }
}
void publishEvent(Event event) {
    // ...
    throw new Exception2();
}
```

## Local variables

```{.java .numberLines .lineAnchors}
// stack frame [this, obj, a, b]
int local1(Object obj) {
    int a = 3;
    Object b = obj.getB();
    possibleThrown();
    obj.setA(a); // obj is affected
    a = 5;       // a is not affected
    b.setA(a);   // b is affected
    Object c = obj.getC();
    c.setA(a);   // c is not affected
}

// stack frame [this, a, b, c]
int local2() {
    int a = 3;
    int b = 0;
    int c = 0;
    try {
        possibleThrown();
        a = 5; // a is tainted
    } catch(Exception e) {
        b = 10; // b is tainted
    }
    c = 5;  // c is not tainted.
    //...
}
```

## Global variables

```{.java .numberLines .lineAnchors}
class {
    int a;
    Object b;
    boolean c;
    int global1() {
        possibleThrown();
        a = 5; // a is tainted
        b.callMethod(); // b is tainted
    }

    int global2() {
        try {
            possibleThrown();
            a = 5; // a is tainted
        } catch (Excpetion e) {
            b.callMethod() // b is tainted
        }
        c = true; // c is not tainted
    }

}
```

## Method Call
```{.java .numberLines .lineAnchors}
class Foo {
    int a;
    static void updateAStatic(Foo f) {
        f.a = 3;
    }

    static int readAStatic(Foo f) {
        return f.a;
    }

    void updateA() {
        a = 3;
    }

    int readA() {
        return a;
    }
}

void test() {
    Foo f1, f2, f3, f4;
    possibleThrown();
    updateAStatic(f2); // f2 is not tainted
    readAStatic(f3); // f3 is not tainted

    f1.updateA(); // f1 is tainted, f1.a is not tainted
    f4.readA(); // f4 is tainted
}
```

## Data Flow

```{.java .numberLines .lineAnchors}
void setup(HTTPClient client, Request r) {
    Cert cert = null;
    try {
        cert = getCert(); // throws RuntimeException
    } catch (Exception e) {
        LOG.error("Cert failure");
    }
    client.setCert(cert.getData()); // NullPointerException
}
```

## Control And Data Flow

```{.java .numberLines .lineAnchors}
void foo() {
    boolean success = false;
    try {
        possibleThrown();
        success = true;
    } catch (Exception e) {
    }

    if (!sucess) {
        anotherPossibleThrown();
    }
}
```


# Exceptions through data flow

An exception is thrown because of the data flow changes
caused by another exception.

## All exceptions are responsible for the failure request

```{.java .numberLines .lineAnchors}
void setup(HTTPClient client, Request r) {
    Cert cert = null;
    try {
        cert = getCert(); // throws RuntimeException
    } catch (Exception e) {
        LOG.error("Cert failure");
    }
    // Other initialization logic
    try {
        client.setCert(cert.getData());
    } catch (Exception e) {
        LOG.error("client failure");
    }
}

void sendRequest(HTTPClient client, Request r) {
    setup(client);
    client.sentRequest(r); // throws RequestException
}

```


```
Exception Chain:
Root -> Exception1:2
     |> NullPointerException:12
     |> RequestException:20
Affected Variable: Exception1:2 (cert(w))
                   NullPointerException:12 (cert(r), client(w))
                   RequestException:20 (client(r))
Failure Exception: RequestException:20
```

[FINERACT-1211](https://issues.apache.org/jira/browse/FINERACT-1211),



# Exceptions through control flow

An exception is thrown because of the control flow changes
caused by another exception.

## All exceptions are responsible for the failure request

```{.java .numberLines .lineAnchors}
Response retryRecursive(Request r, int retryCount) {
    if (retryCount > MAX_RETRY) throw new UnexpectedException();
    try {
        throw new Exception1();
    }
    catch (Exception e) {
        if (e instanceof UnexpectedException) {
            throw e;
        }
        return retryRecursive(r, retryCount + 1);
    }
}
```

```
Exception Tree: Root -> Exception1:4 -> Exception1:4 -> UnexpectedException:2
Failure Exception: UnexpectedException
```

[SOLR-12649](https://issues.apache.org/jira/browse/SOLR-12649),

## Some exceptions are irrelevant to the failure request


- The failure request is thrown before other exceptions

```{.java .numberLines .lineAnchors}
void processRequest(Request r) {
    try {
        throw new Exception1();
    } catch (Exception e) {
        publishEvent(new FailureEvent("failure"));
        throw e
    }
}
void publishEvent(Event event) {
    try {
        throw new Exception2();
    } catch (Exception e) {
        LOG.error(e);
    }
}
```

```
Exception Tree: Root -> Exception1:3 -> Exception2:11
Failure Exception: Exception1:3
```
[FINERACT-1593 (staff API with invalid JSON)](https://issues.apache.org/jira/browse/FINERACT-1593),

- The failure request is thrown after other exceptions

```{.java .numberLines .lineAnchors}
void processRequest(Request r) {
    try {
        throw new Exception1();
    } catch (Exception e) {
        publishEvent(new FailureEvent("failure"));
        throw e;
    }
}
void publishEvent(Event event) {
    throw new Exception2();
}
```

```
Exception Tree: Root -> Exception1:3 -> Exception2:10
Failure Exception: Exception2:10
```
[HADOOP-16742](https://issues.apache.org/jira/browse/HADOOP-16742),

## Exception through indirection

- Exceptions passed through future objects

```java
void process() {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    Future future = executor.run(new Runnable() {
        @Override
        public void run() {
            throw new IllegalStateException();
        }
    });
    try {
        future.get();
    } catch (ExecutorException e) { // Not the original exception is thrown
        throw e.getCause();
    }
}
```
[FLINK-8785](https://issues.apache.org/jira/browse/FLINK-8785)

- Exceptions handled through handlers

```java
public class Main extends Thread {
  public static void main(String[] args) throws InterruptedException {
    Main thread = new Main();
    thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(Thread t, Throwable e) {
            // Handler called in the exception thread
        }
    });
    thread.start();
  }
  public void run() {
  	throw new RuntimeException("error");
  }
}
```

[FLINK-5232](https://issues.apache.org/jira/browse/FLINK-8785)



# Exceptions through both control- and data- flow


## Failure occurs in a single request

```{.java .numberLines .lineAnchors}
void sendRequest(Request r) {
    int retry = 0;
    while (retry < MAX_RETRY) {
        try {
            if (condition) {
                throw new Exception1();
            } else {
                throw new UnexpectedException();
            }
            break;
        }
        catch (Exception e) {
            if (e instanceof UnexpectedException) {
                throw e;
            }
            retry++;
            LOG.error(e);
        }
    }
}
```

```
Exception Tree: Root -> Exception1:6
                     |> Exception1:6
                     |> UnexpectedException:8 -> UnexpectedException:14
Affected Variable: UnexpectedException:14
                   UnexpectedException:8 (retry(r), MAX_RETRY(r))
                   Exception1:6 (retry(w))
                   Exception1:6 (retry(w))
Failure Exception: UnexpectedException:14
```

[HBASE-5003](https://issues.apache.org/jira/browse/HBASE-5003),
[HBASE-17475](https://issues.apache.org/jira/browse/HBASE-17475),
[PHOENIX-5142](https://issues.apache.org/jira/browse/PHOENIX-5142)

## Failure affects both local and global variables

```{.java .numberLines .lineAnchors}
class Channel {
    SSLEngine sslEngine;
    void handshake() {
        // init handshake
        throw IOException();
        // other logic
        sslEngine = new SSLEngine();
    }
    void close() {
        sslEngine.terminate();
    }
}
class Endpoint {
    Channel c = new Channel();
    void doClose() {
        c.close();
    }
    void doRun() {
        int handshake = -1;
        try {
            // some logic
            c.handshake()
            // other logic
            handshake = 0;
        } catch (IOException e) {
            handshake = -1;
        }
        if (handshake == 0) {
            // do something
        }
        else if (handshake == -1) {
            doClose();
        }
    }
}

```

## With threads

```{.java .numberLines .lineAnchors}
Map<Integer, State> stateMap;

void createEventWriter(int jobId) {
    try {
        throw new IOException();
    } catch (Exception e) {
        throw new CreateException(e);
    }
    stateMap[jobId] = new State();
}

void process(int jobId) {
    Thread t = new Thread(createEventWriter());
    t.start();

    //... other wrok

    t.join();
    stateMap[jobId].call(); // NullPointerException is thrown
}
```
```
Exception Tree: Root -> IOException:5 -> CreateException:7
                     |> NullPointerException:19
```
[MAPREDUCE-6654](https://issues.apache.org/jira/browse/MAPREDUCE-6654)

## Failure occurs in multiple requests (stateful)

```{.java .numberLines .lineAnchors}
int totalCommits = 0;
void recoverRequest(Checkpoint checkpoint) {
    try {
        for (commit: checkpoint) {
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
    assert(totalCommits == id+1) // Inconsistent state
    //...
    totalCommits++;
}
```

```
Exception Tree: RecoveryRequest -> Root -> OOMException:6
                CommitRequest -> Root -> AssertionFailure:16
Affected Variable: totalCommits (try block)
```

In this example, the server first receives a recovery request to
recover 10 commits but the recover request fails after 5 commits
due to OOM exception. However, the client assumes that the recover
request finishes successfully. It then sends a commit request with
commit ID to 11 which breaks the assertion in `commitRequest`.

[HDFS-4128](https://issues.apache.org/jira/browse/HDFS-4128),


# Other issues:

- Root cause exceptions are not logged: [JCR-1493](https://issues.apache.org/jira/browse/JCR-1493), [HADOOP-11328](https://issues.apache.org/jira/browse/HADOOP-11328),
[SOLR-4019](https://issues.apache.org/jira/browse/SOLR-4019),
[DERBY-922](https://issues.apache.org/jira/browse/DERBY-922)
- Benign exceptions are logged: [HADOOP-10015](https://issues.apache.org/jira/browse/HADOOP-10015), [HIVE-11062](https://issues.apache.org/jira/browse/HIVE-11062),
[KAFKA-6519](https://issues.apache.org/jira/browse/KAFKA-6519)
