package al.aoli.exchain.instrumentation

import al.aoli.exchain.runtime.NativeRuntime
import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
  // We cannot enable bytebuddy for now because
  // we have to instrument bytebuddy which is slow...
  // If we leave bytebuddy uninstrumented, we will see
  // crashes when bytebuddy gets a multi-dim array
  // from an instrumented class.
  //    AgentBuilder.Default()
  //        .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
  //        .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
  //        .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
  //        .with(object: AgentBuilder.Listener.Adapter() {
  //            override fun onTransformation(
  //                typeDescription: TypeDescription,
  //                classLoader: ClassLoader?,
  //                module: JavaModule?,
  //                loaded: Boolean,
  //                dynamicType: DynamicType
  //            ) {
  //                super.onTransformation(typeDescription, classLoader, module, loaded,
  // dynamicType)
  //                TransformedCodeStore.store[dynamicType.typeDescription.typeName] =
  // dynamicType.bytes
  //            }
  //        })
  // //        .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly())
  //        .disableClassFormatChanges()
  //        .type(not(
  //            nameStartsWith<TypeDescription>("java")
  //                .or(nameStartsWith("jdk"))
  //                .or(nameStartsWith("java"))
  //                .or(nameStartsWith("javax"))
  //                .or(nameStartsWith("sun"))
  //                .or(nameStartsWith("ch.qos"))
  //                .or(nameStartsWith("com.sun"))
  //                .or(nameStartsWith("net.bytebuddy"))
  //                .or(nameStartsWith("org.objectweb"))
  //                .or(nameStartsWith("shadow.asm"))
  //                .or(nameStartsWith("kotlin"))
  //                .or(nameStartsWith("al.aoli.exception.instrumentation"))
  //                .or(nameStartsWith("org.quartz"))
  //                .or(nameStartsWith("com.intellij"))
  //                .or(nameStartsWith("edu.columbia.cs.psl"))
  //                .or(nameContains("FastClassBySpringCGLIB"))
  //                .or(nameContains("\$Proxy")))
  //        )
  //        .transform { builder, type, _, _ ->
  //            builder
  // //            builder
  // ////                .visit(Advice.to(ExceptionAdvices::class.java).on(isMethod()))
  // //                .visit(
  // //                    AsmVisitorWrapper.ForDeclaredMethods()
  // ////                        .writerFlags(ClassWriter.COMPUTE_MAXS or
  // ClassWriter.COMPUTE_FRAMES)
  // ////                        .readerFlags(ClassReader.EXPAND_FRAMES)
  // //                        .invokable(any(), TryCatchBlockTransformer(type.typeName))
  // //                )
  //        }
  //        .installOn(instrumentation)
  NativeRuntime.initializedCallback()
}
