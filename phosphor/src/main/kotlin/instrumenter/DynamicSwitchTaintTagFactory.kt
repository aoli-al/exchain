package al.aoli.exchain.phosphor.instrumenter

import edu.columbia.cs.psl.phosphor.instrumenter.DataAndControlFlowTagFactory

class DynamicSwitchTaintTagFactory: DataAndControlFlowTagFactory() {

    override fun isIgnoredMethod(owner: String, name: String, desc: String): Boolean {
        if (name.endsWith(Constants.originMethodSuffix)) {
            return true
        }
        return false
    }

}