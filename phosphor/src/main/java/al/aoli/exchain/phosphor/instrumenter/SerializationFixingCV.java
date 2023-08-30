// package al.aoli.exchain.phosphor.instrumenter;
//
// import edu.columbia.cs.psl.phosphor.Configuration;
// import edu.columbia.cs.psl.phosphor.control.OpcodesUtil;
// import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
// import edu.columbia.cs.psl.phosphor.org.objectweb.asm.*;
// import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.AnalyzerAdapter;
// import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
// import edu.columbia.cs.psl.phosphor.runtime.Taint;
//
// import java.io.IOException;
// import java.io.Serializable;
//
// import static
// edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_TYPE;
//
//
// public class SerializationFixingCV extends ClassVisitor implements Opcodes {
//
//    // ObjectInputStream class name
//    private static final String INPUT_STREAM_NAME = "java/io/ObjectInputStream";
//    // ObjectOutputStream class name
//    private static final String OUTPUT_STREAM_NAME = "java/io/ObjectOutputStream";
//    // ObjectStreamClass class name
//    private static final String STREAM_CLASS_NAME = "java/io/ObjectStreamClass";
//    // Header byte for serialized objects
//    private static final byte TC_OBJECT = (byte) 0x73;
//    // Header byte serialized null values
//    private static final byte TC_NULL = (byte) 0x70;
//
//    // Name of class being visited
//    private final String className;
//
//    public SerializationFixingCV(ClassVisitor cv, String className) {
//        super(Configuration.ASM_VERSION, cv);
//        this.className = className;
//    }
//
//    @Override
//    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
// String[] exceptions) {
//        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//        if (Configuration.taintTagFactory.isIgnoredMethod(className, name, desc)) {
//            return mv;
//        }
//        if (STREAM_CLASS_NAME.equals(className)) {
//            return new StreamClassMV(mv);
//        }
//    }
//
//
//    @SuppressWarnings("unused")
//    public static Object wrapIfNecessary(Object obj, Taint tag) {
//        if (tag != null && !tag.isEmpty()) {
//            return new TaggedReference(tag, obj);
//        }
//        return obj;
//    }
//
//    @SuppressWarnings("unused")
//    public static Object unwrapIfNecessary(Object ret, PhosphorStackFrame phosphorStackFrame) {
//        if (ret instanceof TaggedReference) {
//            phosphorStackFrame.setReturnTaint(((TaggedReference) ret).tag);
//            return ((TaggedReference) ret).val;
//        }
//        return ret;
//    }
//
//    private static class TaggedReference implements Serializable {
//        Object val;
//        Taint tag;
//
//        TaggedReference(Taint tag, Object val){
//            this.tag = tag;
//            this.val = val;
//        }
//        private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
//            stream.writeObject(val);
//            stream.writeObject(tag);
//        }
//
//        private void readObject(java.io.ObjectInputStream stream) throws IOException,
// ClassNotFoundException {
//            val = stream.readObject();
//            tag = (Taint) stream.readObject();
//        }
//
//    }
//    private static class StreamClassMV extends MethodVisitor {
//
//        StreamClassMV(MethodVisitor mv) {
//            super(Configuration.ASM_VERSION, mv);
//        }
//
//        @Override
//        public void visitMethodInsn(final int opcode, final String owner, final String name, final
// String desc,
//                                    final boolean isInterface) {
//            if (OUTPUT_STREAM_NAME.equals(owner) && name.startsWith("write")) {
//                Type[] args = Type.getArgumentTypes(desc);
//                if (args.length > 0 && Type.getType(Configuration.TAINT_TAG_DESC).equals(args[0]))
// {
//                    String untaintedDesc = desc;
//                    boolean widePrimitive = Type.DOUBLE_TYPE.equals(args[1]) ||
// Type.LONG_TYPE.equals(args[1]);
//                    if (args.length == 2) {
//                        // TODO this is not at all set up for reference tags... but tests pass
// anyway?
//                        // stream, taint, primitive
//                        super.visitInsn(widePrimitive ? DUP2_X1 : DUP_X1);
//                        super.visitInsn(widePrimitive ? POP2 : POP);
//                        super.visitInsn(POP);
//                        super.visitMethodInsn(opcode, owner, name, untaintedDesc, isInterface);
//                        return;
//                    } else if (args.length == 4 && args[3].equals(CONTROL_STACK_TYPE)) {
//                        // Taint primitive taint ControlFlowStack
//                        super.visitInsn(POP);
//                        super.visitInsn(POP);
//                        // Taint primitive
//                        super.visitInsn(widePrimitive ? DUP2_X1 : DUP_X1);
//                        super.visitInsn(widePrimitive ? POP2 : POP);
//                        super.visitInsn(POP);
//                        super.visitMethodInsn(opcode, owner, name, untaintedDesc, isInterface);
//                        return;
//                    }
//                }
//            }
//            super.visitMethodInsn(opcode, owner, name, desc, isInterface);
//        }
//    }
// }
