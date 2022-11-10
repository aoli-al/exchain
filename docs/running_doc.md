# Exchain Running Doc

## Problem

Identifying root cause of program failure is challenging because the root cause may spatially and temporally far from the observable program failure.


## Observation

Some failures are caused by exception mishandling and exception propagation. However, such failures are extremely challenging to debug because the exceptions are not always logged by the developers. Many exceptions are swallowed silently.

## Input

A Java application $P$ which will be deployed in production.

## Output

An exception graph $G=\langle V, E\rangle$, where each vertex $e\in V$ represents an exception thrown in application $P$. Each $E=\langle e_1, e_2\rangle$ represents a causality chain between exception $e_1$ and exception $e_2$ ($e_1$ causes $e_2$).

## Evaluation Metrics

- Performance: the proposed solution should not introduce excessive overhead to the application $P$.
- Accuracy:
    - false positive: the graph $G$ has edge $\langle e_1, e_2\rangle$ but $e_1$ does not cause $e_2$.
    - false negative: $e_1$ causes $e_2$ but edge $\langle e_1, e_2\rangle$ does not in graph $G$.

## Proposed Solution

- Insight: an exception causes data-flow and control-flow changes of the application. Tracking the impact of such changes allows us to build the causality graph.


- Design: given a running application $P$, for each exception $e$ that is thrown, we want to compute:
    -   source variables $S_e$: a set of variables that cause the exception $e$.
    -   affected variables $A_e$: a set variables whose values are affected by the exception $e$.
    -   The propagation of $A_e$ ($Prop(A_e)$): how affected variables affect the state of the program.

We define:

-   Exception $e_1$ is caused by exception $e_2$ if and only if the
    intersection of $S_{e_1}$ and $Prop(A_{e_2})$ is not empty.

Example:

``` {.java .numberLines .lineAnchors}
void setup(HTTPClient client, Request r) {
    Cert cert = null;
    try {
        cert = getCert();
    } catch (Exception e) {
        LOG.error("Cert failure");
    }
    Cert result = cert;
    // Other initialization logic
    try {
        client.setCert(result.getData()); // throws NPE
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
    - source var: none
    - affected var $A_e$: `cert`,
    - $Prop(A_e)$: `result`
-   NullPointerException:10
    -   source var: `result`
    -   affected var: `client`
-   RequestException:20
    -   source var: `client`
    -   affected var: `client`

## Compute Source Variables $S_e$

Input:

- Exception $e$ and its thrown point in application $S$

Output:
- A set of variables $S_e$ of exception $e$


Algorithm:

-   If the exception is NPE
    -   The thrown instruction's callee is the source variable
-   If the exception is OutOfBoundException
    -   The thrown instruction's callee and arguments are the source variables
-   If the exception is an AssertError or the exception is from a throw
    instruction
    -  The variables of the closes branch instruction of the thrown instruction are source variables.
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

Input:
- Exception $e$ and its thrown point in application $S$

Output:
- A set of variables $A_e$ of exception $e$


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

## Compute the Propagation of the Affected Variables $Prop(A_e)$


- Note that weather we are going to use an online algorithm or an offline algorithm affects whether we need to compute the $A_e$ and $S_e$ at runtime.

### Online Algorithm (dynamic analysis)

- Compute the propagation of the affected variables during the execution of the application.

- End result is from dynamic analysis. Use static analysis to improve performance.
    - Main challenge: instrumenting the application to track the propagation of variables introduces tremendous overhead (>400%).
    - Potential Solutions: only enable taint analysis when it is required
        - Temporal
            - dynamically enable and disable taint analysis.
                1. need to understand when to enable/disable taint analysis
                2. implement functionalities to disable/enable taint analysis dynamically.
        - Spacial
            - Only enable dynamic taint analysis for __required__ libraries.
            - Program slicing?

### Offline Algorithm (static analysis)

- Compute the propagation of the affected variables after the execution of the application.

- End result is from static analysis. Use dynamic analysis to remove false positives.
    - Main challenge: imprecise static analysis produces false negatives
    - Idea: use dynamic analysis to collect information to reduce false positives.
    - Issues:
        - False positives are unknown unknowns. Theoretically, the static analysis can never avoid false positives.