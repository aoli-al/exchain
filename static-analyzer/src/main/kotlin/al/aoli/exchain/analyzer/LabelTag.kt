package al.aoli.exchain.analyzer

import java.nio.ByteBuffer
import soot.tagkit.Tag

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
