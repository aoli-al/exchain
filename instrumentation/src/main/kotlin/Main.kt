package al.aoli.exception.instrumentation

import al.aoli.exception.instrumentation.runtime.ExceptionAdvices
import al.aoli.exception.instrumentation.transformers.ExceptionBlockTransformer
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import net.bytebuddy.utility.JavaModule
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
    AgentBuilder.Default()
        .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
        .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
        .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
        .with(object: AgentBuilder.Listener.Adapter() {
            override fun onTransformation(
                typeDescription: TypeDescription,
                classLoader: ClassLoader?,
                module: JavaModule?,
                loaded: Boolean,
                dynamicType: DynamicType
            ) {
                super.onTransformation(typeDescription, classLoader, module, loaded, dynamicType)
                File("/tmp/${dynamicType.typeDescription.typeName}.class").writeBytes(dynamicType.bytes);
            }
        })
        .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly())
        .disableClassFormatChanges()
        .type(nameStartsWith("al.aoli.exception.demo"))
//        .type(not(nameStartsWith<TypeDescription>("al.aoli")
//            .or(nameStartsWith("java.util"))
//            .or(nameStartsWith("java.lang"))))
        .transform(
            AgentBuilder.Transformer.ForAdvice().advice(not(isConstructor()), ExceptionAdvices::class.java.name))
        .transform { builder, type, _, _ ->
            builder
                .visit(
                    AsmVisitorWrapper.ForDeclaredMethods()
                        .writerFlags(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
                        .readerFlags(ClassReader.EXPAND_FRAMES)
                        .invokable(any(), ExceptionBlockTransformer(type.typeName))
                )
        }
        .installOn(instrumentation)
}