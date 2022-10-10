package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarResult
import com.google.gson.Gson
import soot.G
import soot.options.Options
import java.io.File


fun setupSoot(sourceDirectory: String) {
    val paths = File(sourceDirectory).readText().split(":").map { it.trim() }
    G.reset()
    Options.v().set_process_dir(paths)
    Options.v().set_prepend_classpath(true)
    Options.v().set_keep_line_number(true)
    Options.v().set_keep_offset(true)
//    Options.v().set_whole_program(true)
    Options.v().set_prepend_classpath(true)
    Options.v().set_allow_phantom_refs(true)
    Options.v().set_write_local_annotations(true)
    Options.v().setPhaseOption("jb", "use-original-names:true")
    Options.v().setPhaseOption("jb", "keep-offset:true")
}

fun loadAndProcess(dataDirectory: String) {
    val path = File("$dataDirectory/latest").readText()
    val data = File("$dataDirectory/$path/affected-var-results.json").readText()
    val results = mutableListOf<AffectedVarResult>()
    for (s in data.split("\n")) {
        if (s.isNotEmpty()) {
            results.add(Gson().fromJson(s, AffectedVarResult::class.java))
        }
    }
    val analyzer = Analyzer(results)
    analyzer.process()
    File("$dataDirectory/$path/dependency.json").writeText(Gson().toJson(analyzer.exceptionGraph))
}

fun main(argv: Array<String>) {
    setupSoot(argv[0])
    loadAndProcess(argv[1])
}