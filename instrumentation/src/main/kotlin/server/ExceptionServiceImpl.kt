package al.aoli.exception.instrumentation.server

import java.rmi.Remote

interface ExceptionService: Remote {
    fun start()
}

object ExceptionServiceImpl: ExceptionService {
    var started = false
    override fun start() {
        started = true
    }
}