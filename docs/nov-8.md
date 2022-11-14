# Taint Analysis

## How taint analysis is implemented.

``` {.java .numberLines .lineAnchors}
class Foo {
    int v;
}
Foo origin(int a) {
    Foo f = new Foo();
    f.v = a + 5;

    Foo f2 = new Foo();
    f2 = f;
    return f2;
}

class Foo {
    int v;
    Tag v_tag;
    Tag this_tag;
}
Tuple<Foo, Tag> instrumented(int a, Tag a_tag) {
    Foo f = new Foo();
    Tag f_tag = emptyTag();
    f.v = a + 5;
    f.v_tag = a_tag;

    Foo f2 = new Foo();
    Tag f2_tag = emptyTag();

    f2 = f;
    f2_tag = f1_tag;

    return (f2, f2_tag);
}
```

## Performance Issue

The taint analysis framework makes two modifications to the original application.

1. For each stack variable, it creates a shadow stack variable that tracks the taint tag of that variable. For example, `f_tag` for `f`.
2. For each field of a class, it creates a shadow field that tracks the taint tag for that field.


- Observation: The overhead is mainly introduced by maintaining taint tags for **stack variables**.

What if we only add taint tags for class fields?

``` {.java .numberLines .lineAnchors}
class Foo {
    int v;
    Tag v_tag;
    Tag this_tag;
}
Foo instrumented(int a) {
    Foo f = new Foo();
    f.v = a + 5;

    Foo f2 = new Foo();

    f2 = f;
    return f2;
}
```

If we want to add some taints:


``` {.java .numberLines .lineAnchors}
Foo instrumented(int a) {
    Foo f = new Foo();
    f.v = a + 5;
    f.v_tag = "taint!";

    Foo f2 = new Foo();

    f2 = f;
    return f2;
}
```

Note that `f2.v_tag` also has the taint information because they point to the same heap object! No overhead is introduced!

## Idea


``` {.java .numberLines .lineAnchors}
HTTPClient create() {
    //...
    HTTPClient c = new HTTPClient();
    try {
        c.setCert(cert.getData()); // cert throws NPE
    } catch (Exception e) {
        LOG.error("client failure");
    }
    return c;
}
void sendRequest(Request r) {
    HTTPClient client = create();
    if (!client.hasCert()) {
        throw new RequestException();
    }
    client.sendRequest(r);
}
```

- When the `NPE:5` thrown, we can taint the heap object `new HTTPClient():3` with the exception information `NPE:5`.
    - `cert.this_tag = "NPE:5"`
- When the `RequestException:14` thrown, we identify `client` is the source variable. We further check the heap object that `client` points to has taint information `NPE:5`. Then we construct the causality chain `NPE:5 -> RequestException:14`

## Issues

- We cannot handle null pointers. In the following example, if we only taint heap objects, we cannot find the object that `cert` points to.


``` {.java .numberLines .lineAnchors}
HTTPClient create() {
    Cert cert = null;
    try {
        cert = getCert();
    } catch (Exception e) {
        LOG.error("Cert failure");
    }
}
Cert getCert() {
    throw new RuntimeException(); // throws NPE
}
```

- We cannot track the propagation along heap objects. In the following example, if `b.v` is tainted. The taint information will be missing at line 6.
    - However, in our case study we don't need to propagate taint information for heap objects.


``` {.java .numberLines .lineAnchors}
class Foo {
    int v;
}
Foo a = new Foo();
Foo b = new Foo();
a.v = b.v + 3;
```

## Potential Solution

We may want to track different types of variables differently.

- If the affected variables are primitive values, null pointers, we use static data-flow analysis
    - The sink and source in static data-flow analysis are both primitive values or null values.
- If the affected variables are heap objects, class fields, we taint the heap object dynamically.

This is based on my limited experiment results:

- Static is good at tracking stack variables and bad at tracking heap objects.
    - All false positives are caused by heap objects.


Our result is a combination of static and dynamic analysis. For example given the complete example above:

``` {.java .numberLines .lineAnchors}
HTTPClient create() {
    Cert cert = null;
    try {
        cert = getCert();
    } catch (Exception e) {
        LOG.error("Cert failure");
    }
    HTTPClient c = new HTTPClient();
    // Other initialization logic
    try {
        c.setCert(cert.getData()); // throws NPE
    } catch (Exception e) {
        LOG.error("client failure");
    }
    return c;
}
Cert getCert() {
    throw new RuntimeException(); // throws NPE
}
void logAndThrow(String msg) {
    logger.log(msg);
    throw new RequestException(msg);
}
void sendRequest(Request r) {
    HTTPClient client = create();
    if (!client.hasCert()) {
        logAndThrow("no cert");
    }
    client.sendRequest(r);
}
```

We have:

- `RuntimeException:18`
    - Source Var: None
    - Affected Var: `cert->null`
        - the `cert` variable points to `null` and we use offline static analysis to compute the propagation. We add `cert` to sources in the static data-flow analysis.
- `NullPointerException:11`
    - Source Var: `cert`
        - `cert` is null and we don't build any causality chain at runtime for `NullPointerException:11`. We add `cert` to sinks in the static data-flow analysis.
    - Affected Var: `c->HTTPClient:8`
        - the affected variable `c` variable points to `HTTPClient:8` heap object. We assign the taint tag `c.this_tag.add("NPE:11")` to the heap object.
- `RequestException:22`
    - Source Var: `client`
        - the source variable `client` variable points to `HTTPClient:8` heap object. We look at all tags associated with the heap object and construct the causality chain `NPE:11 -> RequestException:22`.
    - Affected Var: `client->HTTPClient:8`
        - the affected variable `client` variable points to `HTTPClient:8` heap object. We assign the taint tag `c.this_tag.add("NPE:11")` to the heap object.

- Overhead estimation:
    - If no exception is thrown, no overhead is introduced
    - When an exception is thrown, overhead = time to compute affected var + time to compute source var



- What we cannot handle:
    - We cannot handle taint propagations between stack variables and heap objects.

``` {.java .numberLines .lineAnchors}
class Foo {
    int a = 3;
}
Foo f1 = new Foo(); // f1.a is tainted.
int b = f1.a;
Foo f2 = new Foo();
f2.a = b;
if (f2.a) {
    crash();
}
```
    - There can still be false positives reported by the data-flow analysis.

## Appendix (False Positive Reported By Static Analysis)

- `r1.read():28` triggers the first exception `IOException`
    - The affected variable is `r1.maxNextCharInd`
    - The source variable is `r1.maxNextCharInd`
- `r2.read():33` triggers the second exception `IOException`
    - The affected variable is `r2.maxNextCharInd`
    - The source variable is `r2.maxNextCharInd`
- Our static data-flow analysis cannot distinguish `r1` and `r2` and believes `r1.maxNextCharInd == r2.maxNextCharInd`. The static analysis returns a false causality chain `IOException1 -> IOException2`.

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

## Source Vars

- Out of 5k exceptions, only 2 exceptions don't have source variables.
    - NoSuchMethodError

```{.java .numberLines .lineAnchors}
private static MDCAdapter bwCompatibleGetMDCAdapterFromBinder() throws NoClassDefFoundError {
    try {
        return StaticMDCBinder.getSingleton().getMDCA();
    } catch (NoSuchMethodError nsme) {
        // binding is probably a version of SLF4J older than 1.7.14
        return StaticMDCBinder.SINGLETON.getMDCA();
    }
}
```

