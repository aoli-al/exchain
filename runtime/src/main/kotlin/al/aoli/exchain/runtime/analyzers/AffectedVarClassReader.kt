package al.aoli.exchain.runtime.analyzers

import org.objectweb.asm.ClassReader

// ASM ClassReader does not tell MethodVisitor
// the offset of each instruction. We need this
// information for data-flow analysis. In
// order to reconstruct the offset, we need
// to compute them manually.
// However, LDC, LDC_W are both translated
// to LDC in ClassReader. To distinguish them,
// we monitor the usage of readUnsignedShort method.
// If the readUnsignedShort method is called before
// creating a LDC instruction, a LDC_W is used.
class AffectedVarClassReader : ClassReader {
    constructor(byteCode: ByteArray) : super(byteCode)
    constructor(className: String) : super(className)

    var lastReadConstIndex = -1
    var lastUnsignedShortOffset = -1

    override fun readConst(constantPoolEntryIndex: Int, charBuffer: CharArray?): Any {
        lastReadConstIndex = constantPoolEntryIndex
        return super.readConst(constantPoolEntryIndex, charBuffer)
    }

    override fun readUnsignedShort(offset: Int): Int {
        lastUnsignedShortOffset = offset
        return super.readUnsignedShort(offset)
    }
}
