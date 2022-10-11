package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarResult
import com.google.gson.Gson
import mu.KotlinLogging
import polyglot.ast.Labeled
import soot.AmbiguousMethodException
import soot.G
import soot.Scene
import soot.SootClass
import soot.jimple.infoflow.Infoflow
import soot.options.Options
import java.io.File

private val logger = KotlinLogging.logger {}
fun setupSoot(sourceDirectory: String): List<String> {
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
    Options.v().set_drop_bodies_after_load(false)
    return paths
}

fun loadAndProcess(args: List<String>) {
    val libs = setupSoot(args[0])
    val dataDirectory = args[1]

    val path = File("$dataDirectory/latest").readText()
    val data = File("$dataDirectory/$path/affected-var-results.json").readText()
    val results = mutableListOf<AffectedVarResult>()
    for (s in data.split("\n")) {
        if (s.isNotEmpty()) {
            results.add(Gson().fromJson(s, AffectedVarResult::class.java))
        }
    }

    val infoFlow = Infoflow("", false, null)
    infoFlow.setThrowExceptions(true)
    infoFlow.setSootConfig { options, configs ->
        options.set_allow_phantom_refs(true)
        options.set_prepend_classpath(true)
        options.set_process_dir(libs)
        options.set_output_format(Options.output_format_none)
        options.set_keep_offset(true)
        options.set_keep_line_number(true)
        options.setPhaseOption("jb", "use-original-names:true")
        options.set_ignore_classpath_errors(true)
        options.set_drop_bodies_after_load(false)
//        options.set_ignore_resolution_errors(true)
        options.set_ignore_resolving_levels(true)
    }

    val libPath = libs.joinToString(File.pathSeparator)
    val sourceVarAnalyzer = SourceVarAnalyzer(results)
    val exceptionGraph = mutableMapOf<Int, MutableSet<Int>>()

    for (result in results) {
        Scene.v().forceResolve(result.getSootClassName(), SootClass.BODIES)
        val clazz = Scene.v().getSootClass(result.getSootClassName())
        clazz.setApplicationClass()
        Scene.v().loadNecessaryClasses()
        sourceVarAnalyzer.disabledLabels.add(result.label)
        try {
             val method = clazz.getMethod(result.getSootMethodSubsignature())
            infoFlow.computeInfoflow(libPath, libPath, method.signature, SourceSinkManager(method, result, sourceVarAnalyzer))
        } catch (e: RuntimeException) {
            logger.warn("Failed to get method: ${result.getSootMethodSubsignature()}", e)
            continue
        }

        if (!infoFlow.results.isEmpty) {
            for (sourceSinkInfo in infoFlow.results.results) {
                val definition = sourceSinkInfo.o1.definition
                if (definition is LabeledSinkDefinition) {
                    for (i in definition.label) {
                        exceptionGraph.getOrPut(i) { mutableSetOf() }.add(result.label)
                    }
                }
            }
        }
    }
    File("$dataDirectory/$path/dependency.json").writeText(Gson().toJson(exceptionGraph))
}

fun main(argv: Array<String>) {
    loadAndProcess(argv.asList())
}