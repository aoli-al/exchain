package al.aoli.exchain.analyzer

import al.aoli.exchain.runtime.analyzers.AffectedVarDriver
import al.aoli.exchain.runtime.analyzers.ExceptionLogger
import al.aoli.exchain.runtime.objects.AffectedVarResult
import al.aoli.exchain.runtime.objects.Type
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import mu.KotlinLogging
import soot.G
import soot.jimple.infoflow.Infoflow
import soot.jimple.infoflow.InfoflowConfiguration
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode
import soot.options.Options
import java.io.File
import java.lang.NullPointerException
import kotlin.system.exitProcess

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
        configs.memoryThreshold = 0.2
        configs.enableExceptionTracking = false
        configs.enableArrayTracking = false
        configs.flowSensitiveAliasing = false
        configs.staticFieldTrackingMode = StaticFieldTrackingMode.None
        configs.solverConfiguration.dataFlowSolver =
            InfoflowConfiguration.DataFlowSolver.GarbageCollecting
        configs.pathConfiguration.pathBuildingAlgorithm =
            InfoflowConfiguration.PathBuildingAlgorithm.ContextInsensitiveSourceFinder
        configs.pathConfiguration.pathReconstructionTimeout = 2 * 60
        configs.codeEliminationMode = InfoflowConfiguration.CodeEliminationMode.NoCodeElimination
        configs.aliasingAlgorithm = InfoflowConfiguration.AliasingAlgorithm.None
        configs.dataFlowTimeout = 10 * 60
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
                        origin.isThrownInsn
                    )
                result?.label = origin.label
                result
            }
            .filterNotNull()
    val libPath = libs.joinToString(File.pathSeparator)
    val sourceVarAnalyzer = SourceVarAnalyzer(processedResults)
    val dependencyFile = File("$dataDirectory/$path/dependency.json")
    val dependencies =
        if (dependencyFile.exists()) {
            Gson().fromJson(dependencyFile.readText(), Dependencies::class.java)
        } else {
            Dependencies()
        }

    for (result in processedResults) {
        sourceVarAnalyzer.disabledLabels.add(result.label)
        if (dependencies.processed.contains(result.getSignature())) continue

        if (result.affectedLocalName.isEmpty() && result.affectedFieldName.isEmpty()) continue
        logger.info("Start analysing ${result.label}.")
        try {
            infoFlow.computeInfoflow(
                libPath,
                libPath,
                result.getSootMethodSignature(),
                SourceSinkManager(result.getSootMethodSignature(), result, sourceVarAnalyzer)
            )
        } catch (e: RuntimeException) {
            logger.warn("Failed to get method: ${result.getSootMethodSubsignature()}", e)
            infoFlow.config.sootIntegrationMode =
                InfoflowConfiguration.SootIntegrationMode.UseExistingCallgraph
            continue
        }
        infoFlow.config.sootIntegrationMode =
            InfoflowConfiguration.SootIntegrationMode.UseExistingCallgraph
        dependencies.processed.add(result.getSignature())

        if (!infoFlow.results.isEmpty) {
            for (sourceSinkInfo in infoFlow.results.results) {
                val definition = sourceSinkInfo.o1.definition
                if (definition is LabeledSinkDefinition) {
                    for (i in definition.label) {
                        dependencies.exceptionGraph
                            .getOrPut(i) { mutableSetOf() }
                            .add(Pair(result.label, sourceSinkInfo.toString()))
                        println("Dependency $i --------> ${result.label}")
                    }
                }
            }
        }
        File("$dataDirectory/$path/dependency.json").writeText(gson.toJson(dependencies))
    }
    infoFlow.abortAnalysis()
    ExceptionLogger.stop()
    exitProcess(0)
}

class AnalyzerOptions : CliktCommand() {
    val classPath: String by argument(help = "Project classpath")
    val reportPath: String by argument(help = "Production report path")

    override fun run() {
        loadAndProcess(this)
    }
}

fun main(args: Array<String>) = AnalyzerOptions().main(args)
