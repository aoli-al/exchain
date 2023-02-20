package al.aoli.exchain.runtime

object NativeRuntime {
    external fun initializedCallback()
    external fun registerWorkingThread(t: Thread)
    external fun unregisterWorkingThread(t: Thread)

    external fun shutdown()
}
