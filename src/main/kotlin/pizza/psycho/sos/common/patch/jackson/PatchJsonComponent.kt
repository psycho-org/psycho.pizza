package pizza.psycho.sos.common.patch.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.type.TypeFactory
import org.springframework.boot.jackson.JsonComponent
import pizza.psycho.sos.common.patch.Patch

@JsonComponent
object PatchJsonComponent {
    /**
     * Patch<T> 전용 Jackson deserializer.
     *
     * - 필드가 JSON에 존재하지 않는 경우에는 호출되지 않고, 필드 기본값이 사용된다.
     * - 필드가 존재할 때만 호출되며:
     *   - 값이 null 이면 Patch.Clear
     *   - 값이 non-null 이면 Patch.Value(value)
     */
    class PatchDeserializer(
        private val valueType: JavaType? = null,
    ) : JsonDeserializer<Patch<*>>(),
        ContextualDeserializer {
        override fun createContextual(
            ctxt: DeserializationContext,
            property: BeanProperty?,
        ): JsonDeserializer<*> {
            // property.type == Patch<Something> 인 타입 정보
            val patchType: JavaType? = property?.type ?: ctxt.contextualType

            val contained: JavaType =
                if (patchType != null && patchType.containedTypeCount() == 1) {
                    patchType.containedType(0)
                } else {
                    TypeFactory.unknownType()
                }

            return PatchDeserializer(contained)
        }

        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext,
        ): Patch<*> {
            val node: JsonNode = p.codec.readTree(p)

            // "field": null  -> Clear
            if (node is NullNode || node.isNull) {
                return Patch.Clear
            }

            // "field": value -> Value(value)
            val effectiveType = valueType ?: TypeFactory.unknownType()

            @Suppress("UNCHECKED_CAST")
            val value = ctxt.readTreeAsValue(node, effectiveType) as Any
            return Patch.Value(value)
        }

        override fun getNullValue(ctxt: DeserializationContext): Patch<*> = Patch.Clear
    }
}
