package al.aoli.exchain.instrumentation

import al.aoli.exchain.instrumentation.server.ExceptionService
import java.rmi.registry.LocateRegistry

fun main() {
    val service = LocateRegistry.getRegistry("localhost", 9898).lookup(ExceptionService::class.simpleName) as ExceptionService
    service.start()
    println("Instrumentation Started!!!!!!!!!!!!!")
}
