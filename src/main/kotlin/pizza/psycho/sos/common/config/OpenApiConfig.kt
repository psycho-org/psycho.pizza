package pizza.psycho.sos.common.config

import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.ResponseStatus
import pizza.psycho.sos.common.handler.ErrorResponse
import pizza.psycho.sos.common.response.ApiResponse
import pizza.psycho.sos.common.response.OffsetPageInfo
import pizza.psycho.sos.common.response.OffsetPagedApiResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import io.swagger.v3.oas.models.responses.ApiResponse as SwaggerApiResponse

@Configuration
@OpenAPIDefinition(
    info = Info(title = "Psycho API", version = "v1"),
    security = [SecurityRequirement(name = "bearerAuth")],
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
)
class OpenApiConfig {
    private val deferredSchemas = ConcurrentHashMap<String, Schema<*>>()

    @Bean
    fun apiResponseOperationCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            val returnType = handlerMethod.method.genericReturnType
            if (returnType !is ParameterizedType) return@OperationCustomizer operation

            val rawType = returnType.rawType as? Class<*> ?: return@OperationCustomizer operation
            if (!ApiResponse::class.java.isAssignableFrom(rawType)) return@OperationCustomizer operation

            val dataType: Type = returnType.actualTypeArguments[0]
            val isPaged = OffsetPagedApiResponse::class.java.isAssignableFrom(rawType)

            val resolved = ModelConverters.getInstance().readAllAsResolvedSchema(dataType)
            resolved?.referencedSchemas?.forEach { (name, schema) ->
                deferredSchemas[name] = schema
            }

            val dataSchema = resolved?.schema
            val composite = buildCompositeSchema(dataSchema, isPaged)

            val responseStatus = handlerMethod.getMethodAnnotation(ResponseStatus::class.java)
            val statusCode = responseStatus?.value?.value()?.toString() ?: "200"

            // @Operation 어노테이션으로 직접 지정된 응답이 있으면 덮어쓰지 않음
            if (operation.responses?.containsKey(statusCode) != true) {
                val content = Content().addMediaType("application/json", MediaType().schema(composite))
                operation.responses.addApiResponse(
                    statusCode,
                    SwaggerApiResponse().description("Success").content(content),
                )
            }

            addErrorResponses(operation)

            operation
        }

    @Bean
    fun schemaRegistrationCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            val components = openApi.components ?: Components().also { openApi.components = it }
            val schemas = components.schemas ?: mutableMapOf<String, Schema<*>>().also { components.schemas = it }

            val errorResolved = ModelConverters.getInstance().readAllAsResolvedSchema(ErrorResponse::class.java)
            errorResolved?.referencedSchemas?.forEach { (name, schema) -> schemas.putIfAbsent(name, schema) }
            errorResolved?.schema?.let { schemas.putIfAbsent("ErrorResponse", it) }

            val pageInfoResolved = ModelConverters.getInstance().readAllAsResolvedSchema(OffsetPageInfo::class.java)
            pageInfoResolved?.schema?.let { schemas.putIfAbsent("OffsetPageInfo", it) }

            deferredSchemas.forEach { (name, schema) -> schemas.putIfAbsent(name, schema) }
        }

    private fun buildCompositeSchema(
        dataSchema: Schema<*>?,
        isPaged: Boolean,
    ): ObjectSchema {
        val composite = ObjectSchema()
        composite.addProperty("timestamp", StringSchema().format("date-time"))
        composite.addProperty("status", IntegerSchema().example(200))
        composite.addProperty("message", StringSchema().example("success"))

        when {
            isPaged -> {
                composite.addProperty("data", ArraySchema().items(dataSchema))
                composite.addProperty("pageInfo", Schema<Any>().`$ref`("#/components/schemas/OffsetPageInfo"))
            }
            dataSchema != null && hasProperties(dataSchema) -> {
                composite.addProperty("data", dataSchema)
            }
            else -> {
                val emptyObj = ObjectSchema()
                emptyObj.nullable = true
                composite.addProperty("data", emptyObj)
            }
        }

        return composite
    }

    private fun hasProperties(schema: Schema<*>): Boolean = schema.`$ref` != null || !schema.properties.isNullOrEmpty()

    private fun addErrorResponses(operation: io.swagger.v3.oas.models.Operation) {
        val errorRef = Schema<Any>().`$ref`("#/components/schemas/ErrorResponse")
        val errorCodes =
            mapOf(
                "400" to "Bad Request",
                "401" to "Unauthorized",
                "404" to "Not Found",
                "500" to "Internal Server Error",
            )
        errorCodes.forEach { (code, desc) ->
            if (operation.responses?.containsKey(code) != true) {
                val content = Content().addMediaType("application/json", MediaType().schema(errorRef))
                operation.responses.addApiResponse(code, SwaggerApiResponse().description(desc).content(content))
            }
        }
    }
}
