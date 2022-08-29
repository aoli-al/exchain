package al.aoli.exchain.runtime.analyzers

class AffectedVarResult(val affectedVars: IntArray, val affectedFields: Array<String>, val sourceVars: IntArray,
                        val sourceFields: Array<String>)