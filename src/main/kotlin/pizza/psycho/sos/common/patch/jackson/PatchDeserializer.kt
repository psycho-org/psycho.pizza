package pizza.psycho.sos.common.patch.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import pizza.psycho.sos.common.patch.Patch

/**
 * Patch<T> 전용 Jackson deserializer.
 *
 * - 필드가 JSON에 존재하지 않는 경우에는 호출되지 않고, 필드 기본값이 사용된다.
 * - 필드가 존재할 때만 호출되며:
 *   - 값이 null 이면 Patch.Clear
 *   - 값이 non-null 이면 Patch.Value(value)
 */
class PatchDeserializer : JsonDeserializer<Patch<Any?>>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Patch<Any?> {
        val node: JsonNode = p.codec.readTree(p)

        // "field": null  -> Clear
        if (node.isNull) {
            return Patch.Clear
        }

        // "field": value -> Value(value)
        val valueType = ctxt.contextualType?.containedTypeOrUnknown(0)
        val rawClass = valueType?.rawClass

        val value: Any? = ctxt.readTreeAsValue(node, rawClass)
        return Patch.Value(value)
    }
}
