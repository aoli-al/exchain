package al.aoli.exchain.analyzer

import soot.jimple.infoflow.data.SootMethodAndClass
import soot.jimple.infoflow.sourcesSinks.definitions.AbstractSourceSinkDefinition
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition

class LabeledSinkDefinition(am: SootMethodAndClass, val label: Set<Int>): MethodSourceSinkDefinition(am) {
}