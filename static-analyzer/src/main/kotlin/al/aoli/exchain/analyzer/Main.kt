package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarDriver
import al.aoli.exchain.runtime.analyzers.ExceptionLogger
import al.aoli.exchain.runtime.objects.AffectedVarResult
import al.aoli.exchain.runtime.objects.Type
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.lang.NullPointerException
import kotlin.system.exitProcess
import mu.KotlinLogging
import soot.G
import soot.jimple.infoflow.Infoflow
import soot.jimple.infoflow.entryPointCreators.SequentialEntryPointCreator
import soot.options.Options

private val logger = KotlinLogging.logger {}

fun setupSoot(sourceDirectory: String): List<String> {
    val paths = listOf(sourceDirectory)
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

fun loadAndProcess(options: AnalyzerOptions) {
    val libs = setupSoot(options.classPath)
    val dataDirectory = options.reportPath
    val entryPoints = options.programEntryPoint.split(":").toList()
    AffectedVarDriver.instrumentedClassPath = options.classPath
    AffectedVarDriver.type = Type.Dynamic

    val path = File("$dataDirectory/latest").readText().trim()
    val data = File("$dataDirectory/$path/affected-var-results.json").readText()
    val results = mutableListOf<AffectedVarResult>()
    for (s in data.split("\n")) {
        if (s.isNotEmpty()) {
            results.add(Gson().fromJson(s, AffectedVarResult::class.java))
        }
    }

    val gson = GsonBuilder().setPrettyPrinting().create()

    val infoFlow = Infoflow("", false, null)
    infoFlow.setThrowExceptions(false)
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
        options.set_ignore_resolution_errors(true)
        options.set_ignore_resolving_levels(true)
        configs.memoryThreshold = 0.8
        /* configs.staticFieldTrackingMode = StaticFieldTrackingMode.None */
        /* configs.implicitFlowMode = ImplicitFlowMode.NoImplicitFlows */
        /* configs.pathConfiguration.pathReconstructionMode = InfoflowConfiguration.PathReconstructionMode.NoPaths */
        /* configs.solverConfiguration.dataFlowSolver = */
        /* InfoflowConfiguration.DataFlowSolver.FlowInsensitive */
        /* configs.pathConfiguration.pathBuildingAlgorithm = */
        /* InfoflowConfiguration.PathBuildingAlgorithm.ContextInsensitiveSourceFinder */
        /* configs.aliasingAlgorithm = InfoflowConfiguration.AliasingAlgorithm.PtsBased */
        /* configs.enableExceptionTracking = false */
        /* configs.enableArrayTracking = false */
        /* configs.enableArraySizeTainting = false */
        configs.dataFlowTimeout = 6 * 60 * 60
        configs.pathConfiguration.pathReconstructionTimeout = 30 * 60
    }

    val processedResults =
        results
            .map { origin ->
                val dummyException =
                    if ("ClassNotFoundException" in origin.exceptionType) {
                        ClassNotFoundException()
                    } else if ("NullPointerException" in origin.exceptionType) {
                        NullPointerException()
                    } else if ("IndexOutOfBoundsException" in origin.exceptionType) {
                        IndexOutOfBoundsException()
                    } else {
                        java.lang.RuntimeException()
                    }
                val result =
                    AffectedVarDriver.analyzeAffectedVar(
                        dummyException,
                        origin.clazz,
                        origin.method,
                        origin.throwIndex,
                        origin.catchIndex,
                        origin.isThrownInsn)
                result?.label = origin.label
                result
            }
            .filterNotNull()
    val libPath = libs.joinToString(File.pathSeparator)
    val outFile =
        if (options.naive) {
            "dependency.naive.json"
        } else {
            "dependency.json"
        }
    val dependencyFile = File("$dataDirectory/$path/$outFile")
    val dependencies =
        if (dependencyFile.exists()) {
            Gson().fromJson(dependencyFile.readText(), Dependencies::class.java)
        } else {
            Dependencies()
        }

    val sootEntryPoints = mutableListOf<String>()

    if (options.naive) {
        sootEntryPoints.add("<${options.programEntryPoint}: void main(java.lang.String[])>")
    } else {
        var lastProcessedResult: AffectedVarResult? = null
        var shouldAdd = false
        var currentException = -1
        for (processedResult in processedResults) {
            if (currentException != processedResult.label) {
                if (shouldAdd && lastProcessedResult != null) {
                    sootEntryPoints.add(lastProcessedResult.getSootMethodSignature())
                }
                lastProcessedResult = null
                shouldAdd = false
            }
            for (entryPoint in entryPoints) {
                if (processedResult.clazz.startsWith(entryPoint)) {
                    lastProcessedResult = processedResult
                }
            }
            if (processedResult.affectedLocalLine.isNotEmpty() ||
                processedResult.affectedFieldLine.isNotEmpty() ||
                processedResult.affectedStaticFieldLine.isNotEmpty() ||
                processedResult.sourceLines.isNotEmpty()) {
                shouldAdd = true
            }
        }
    }

    try {
        infoFlow.computeInfoflow(
            libPath,
            libPath,
            SequentialEntryPointCreator(sootEntryPoints),
            SourceSinkManager(processedResults, SourceVarAnalyzer(processedResults)))
    } catch (e: RuntimeException) {
        logger.warn("Failed to get method compute infoflow", e)
    }

    if (!infoFlow.results.isEmpty) {
        for (sourceSinkInfo in infoFlow.results.results) {
            println(sourceSinkInfo)
            val definition = sourceSinkInfo.o1.definition
            val sources = sourceSinkInfo.o2.userData
            if (definition is LabeledSinkDefinition && sources is Set<*>) {
                for (from in sources) {
                    if (from !is Int) {
                        continue
                    }
                    val fromIdx = processedResults.indexOfFirst { it.label == from }
                    for (to in definition.label) {
                        val toIdx = processedResults.indexOfFirst { it.label == to }
                        if (fromIdx >= toIdx) continue
                        dependencies.exceptionGraph
                            .getOrPut(to) { mutableSetOf() }
                            .add(Pair(from, sourceSinkInfo.toString()))
                        println("Dependency $from --------> $to")
                    }
                }
            }
        }
    }
    File("$dataDirectory/$path/$outFile").writeText(gson.toJson(dependencies))
    infoFlow.abortAnalysis()
    ExceptionLogger.stop()
    exitProcess(0)
}

class AnalyzerOptions : CliktCommand() {
    val classPath: String by argument(help = "Project classpath")
    val reportPath: String by argument(help = "Production report path")
    val programEntryPoint: String by argument(help = "Program entry point")
    val naive: Boolean by option("--naive").flag(default = false)

    override fun run() {
        loadAndProcess(this)
    }
}

fun main(args: Array<String>) = AnalyzerOptions().main(args)
