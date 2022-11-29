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
