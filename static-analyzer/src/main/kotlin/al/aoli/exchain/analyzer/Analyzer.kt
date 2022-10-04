package al.aoli.exchain.analyzer

import soot.G
import soot.Scene
import soot.options.Options
import java.io.File


fun setupSoot(sourceDirectory: String, entryClasses: List<String>) {
    G.reset()
    Options.v().set_soot_classpath(sourceDirectory)
    Options.v().set_prepend_classpath(true)
    Options.v().set_keep_line_number(true)
    Options.v().set_keep_offset(true)
    for (entryClass in entryClasses) {
        val sc = Scene.v().loadClassAndSupport(entryClass)
        sc.setApplicationClass()
    }
    Scene.v().loadNecessaryClasses()
}

fun main(argv: Array<String>) {
}