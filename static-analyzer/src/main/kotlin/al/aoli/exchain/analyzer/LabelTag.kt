package al.aoli.exchain.analyzer

import soot.tagkit.Tag
import java.nio.ByteBuffer

class LabelTag(val label: Int) : Tag {
    override fun getName(): String {
        return "LabelTag"
    }

    override fun getValue(): ByteArray {
        return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(label).array()
    }

    companion object {
        fun get(label: Int): LabelTag {
            if (label !in tagMap) {
                tagMap[label] = LabelTag(label)
            }
            return tagMap[label]!!
        }

        val tagMap = mutableMapOf<Int, LabelTag>()
    }
}
