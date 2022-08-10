package al.aoli.exchain.phosphor.instrumenter

import al.aoli.exchain.phosphor.instrumenter.Constants.methodNameMapping
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.*
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes.*
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet

class DynamicSwitchPreCV(cv: ClassVisitor, val skipFrames: Boolean): ClassVisitor(Opcodes.ASM9, cv) {

    init {
        var subCV: ClassVisitor? = cv
        while (subCV !is TaintTrackingClassVisitor && subCV != null) {
            val field = ClassVisitor::class.java.getDeclaredField("cv")
            field.isAccessible = true
            subCV = field.get(subCV) as ClassVisitor
        }
        if (subCV is TaintTrackingClassVisitor) {
            val field = subCV.javaClass.getDeclaredField("aggressivelyReduceMethodSize")
            field.isAccessible = true
            val methodList = try {
                field.get(subCV) as HashSet<*>?
            } catch (e: Throwable) {
                println(e.message)
                null
            }

            val newMethodList = HashSet<String>()
            if (methodList != null) {
                for (method in methodList) {
                    newMethodList.add((method as String)
                        .replace(Constants.instrumentedMethodSuffix, "")
                        .replace(Constants.originMethodSuffix, "")
                        .replace("exchainConstructor", "<init>")
                        .replace("exchainStaticConstructor", "<clinit>"))
                }
            }
//            val newMethodList = methodList?.map {
//            }
            field.set(subCV, newMethodList)
        }
    }



    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val newName = methodNameMapping(name)
//        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
//        addSwitch(mv, newName, descriptor, (access and ACC_STATIC) != 0)

//        val mv1 = super.visitMethod(access, newName+Constants.originMethodSuffix, descriptor, signature, exceptions)
        val mv1 = super.visitMethod(access, name, descriptor, signature, exceptions)
//        val mv2 = super.visitMethod(access, name, descriptor, signature, exceptions)
        return ReplayMV(mv1)
    }



}