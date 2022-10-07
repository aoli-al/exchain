package al.aoli.exchain.runtime.analyzers

class AffectedVarResult(var label: Int, val clazz: String, val method: String,
                        val affectedVars: IntArray, val affectedFields: Array<String>,
                        val sourceVars: IntArray, val sourceFields: Array<String>,
                        val branchLines: IntArray)