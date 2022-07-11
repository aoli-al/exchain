package al.aoli.exception.instrumentation

import al.aoli.exception.instrumentation.runtime.ExceptionAdvices
import al.aoli.exception.instrumentation.server.ExceptionService
import al.aoli.exception.instrumentation.server.ExceptionServiceImpl
import al.aoli.exception.instrumentation.transformers.TryCatchBlockTransformer
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import net.bytebuddy.utility.JavaModule
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.Instrumentation
import java.rmi.registry.LocateRegistry
import java.rmi.server.UnicastRemoteObject

fun premain(arguments: String?, instrumentation: Instrumentation) {
    AgentBuilder.Default()
        .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
        .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
        .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
//        .with(object: AgentBuilder.Listener.Adapter() {
//            override fun onTransformation(
//                typeDescription: TypeDescription,
//                classLoader: ClassLoader?,
//                module: JavaModule?,
//                loaded: Boolean,
//                dynamicType: DynamicType
//            ) {
//                super.onTransformation(typeDescription, classLoader, module, loaded, dynamicType)
//                File("/tmp/${dynamicType.typeDescription.typeName}.class").writeBytes(dynamicType.bytes)
//            }
//        })
//        .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly())
        .disableClassFormatChanges()
        .type(not(
            nameStartsWith<TypeDescription>("java")
                .or(nameStartsWith("jdk"))
                .or(nameStartsWith("com.sun"))
                .or(nameStartsWith("net.bytebuddy"))
                .or(nameStartsWith("org.objectweb"))
                .or(nameStartsWith("shadow.asm"))
                .or(nameStartsWith("kotlin"))
                .or(nameStartsWith("al.aoli.exception.instrumentation"))
                .or(nameStartsWith("org.quartz"))
                .or(nameStartsWith("com.intellij"))
                .or(nameContains("FastClassBySpringCGLIB"))
                .or(nameContains("\$Proxy")))
        )

//        .type(nameStartsWith<TypeDescription>("org.apache.fineract")
//            .or(nameStartsWith("org.springframework.cglib.proxy"))
//            .or(nameStartsWith("org.glassfish.jersey.servlet"))
//            .or(nameStartsWith("org.apache.catalina.core"))
//            .or(nameStartsWith<TypeDescription>("al.aoli.exception.demo"))
//            .and(not(nameContains("")))
//            .and(not(nameContains("\$Proxy")))
//        )
        .transform { builder, type, _, _ ->
            builder
                .visit(Advice.to(ExceptionAdvices::class.java).on(isMethod()))
                .visit(
                    AsmVisitorWrapper.ForDeclaredMethods()
                        .writerFlags(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
                        .readerFlags(ClassReader.EXPAND_FRAMES)
                        .invokable(any(), TryCatchBlockTransformer(type.typeName))
                )
        }

        .installOn(instrumentation)

    LocateRegistry.createRegistry(9898).bind(
        ExceptionService::class.simpleName, UnicastRemoteObject.exportObject(
            ExceptionServiceImpl, 0))
}