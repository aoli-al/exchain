package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarResult
import com.google.gson.Gson
import soot.G
import soot.Scene
import soot.options.Options
import java.io.File


fun setupSoot(sourceDirectory: String) {
    G.reset()
    Options.v().set_process_dir(listOf(sourceDirectory))
    Options.v().set_prepend_classpath(true)
    Options.v().set_keep_line_number(true)
    Options.v().set_keep_offset(true)
    Options.v().set_whole_program(true)
    Options.v().set_prepend_classpath(true)
    Options.v().set_allow_phantom_refs(true)
    Scene.v().loadNecessaryClasses()
}

fun loadData(dataDirectory: String): List<AffectedVarResult> {
    val data = File("$dataDirectory/affected-var-results.json").readText()
    val results = mutableListOf<AffectedVarResult>()
    for (s in data.split("\n")) {
        if (s.isNotEmpty()) {
            results.add(Gson().fromJson(s, AffectedVarResult::class.java))
        }
    }
    return results
}

fun main(argv: Array<String>) {
    setupSoot(argv[0])
    val data = loadData(argv[1])
    val analyzer = AffectedVarResultAnalyzer(data)
    analyzer.process()
}