package al.aoli.exchain.instrumentation.runtime

object NativeRuntime {
    external fun initializedCallback()
    external fun registerWorkingThread(t: Thread)
    external fun unregisterWorkingThread(t: Thread)
}