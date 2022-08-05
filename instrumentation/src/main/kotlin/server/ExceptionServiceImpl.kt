package al.aoli.exchain.instrumentation.server

import java.rmi.Remote
import java.rmi.RemoteException

interface ExceptionService: Remote {
    @Throws(RemoteException::class)
    fun start()
}

object ExceptionServiceImpl: ExceptionService {
//    var started = false
    var started = true
    override fun start() {
        started = true
    }
}