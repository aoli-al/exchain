package al.aoli.exchain.instrumentation.transformers

import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.pool.TypePool
import org.objectweb.asm.MethodVisitor
class TryCatchBlockTransformer(private val typeName: String): AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {
    override fun wrap(
        instrumentedType: TypeDescription,
        instrumentedMethod: MethodDescription,
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        typePool: TypePool,
        writerFlags: Int,
        readerFlags: Int
    ): MethodVisitor {
        val access = instrumentedMethod.modifiers
        val name = instrumentedMethod.name
        val descriptor = instrumentedMethod.descriptor
        return CatchBlockTransformer(typeName,
//            methodVisitor,
            TryBlockTransformer(typeName, methodVisitor, access, name, descriptor),
            access, name, descriptor, null,
            instrumentedMethod.exceptionTypes.asErasures().toInternalNames()
        )
    }
}
