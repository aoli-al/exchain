package al.aoli.exchain.instrumentation.analyzers

import java.util.StringJoiner

class AffectedVarResult(val affectedVars: IntArray, val affectedFields: Array<String>, val sourceVars: IntArray,
                        val sourceFields: Array<String>)