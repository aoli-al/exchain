package al.aoli.exchain.runtime.analyzers

object Constants {
    val exceptionHelpers =
        setOf(
            "editLogLoaderPrompt(Ljava/lang/String;Lorg/apache/hadoop/hdfs/server/namenode/MetaRecoveryContext;Ljava/lang/String;)V",
            "logError(Ljava/lang/String;)V"
        )
}
