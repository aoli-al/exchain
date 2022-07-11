package al.aoli.exception.instrumentation

import al.aoli.exception.instrumentation.server.ExceptionService
import al.aoli.exception.instrumentation.server.ExceptionServiceImpl
import java.rmi.registry.LocateRegistry

fun main() {
    val service = LocateRegistry.getRegistry("localhost", 9898).lookup(ExceptionService::class.simpleName) as ExceptionService
    service.start()
}
