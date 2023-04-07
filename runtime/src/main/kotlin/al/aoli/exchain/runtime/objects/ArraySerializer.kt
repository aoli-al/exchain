package al.aoli.exchain.runtime.objects

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class ArraySerializer : JsonSerializer<Array<Pair<Int, SourceType>>> {
  override fun serialize(
      src: Array<Pair<Int, SourceType>>?,
      typeOfSrc: Type?,
      context: JsonSerializationContext?
  ): JsonElement {
    if (src != null) {}
    return JsonPrimitive(0)
  }
}
