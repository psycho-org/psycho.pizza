package pizza.psycho.sos.common.message.channel.mail.template.domain.model.vo

import org.slf4j.LoggerFactory
import pizza.psycho.sos.common.message.channel.mail.template.domain.spec.MailTemplateVariableSpec

class MailTemplateVariables private constructor(
    private val values: Map<String, String?>,
) {
    fun validatePlaceholders(
        texts: List<String>,
        definitions: List<MailTemplateVariableSpec>,
    ) {
        val defined = definitions.map { it.name }.toSet()
        val placeholders =
            texts
                .flatMap { text -> PLACEHOLDER_REGEX.findAll(text).map { it.groupValues[1] }.toList() }
                .toSet()
        val unknown = placeholders - defined

        if (unknown.isNotEmpty()) {
            logger.warn("Undefined placeholders detected: {}", unknown.joinToString(", "))
        }
    }

    fun validateRequired(definitions: List<MailTemplateVariableSpec>) {
        val missingRequired =
            definitions
                .filter { it.required }
                .filter { values[it.name].isNullOrBlank() }
                .map { it.name }

        require(missingRequired.isEmpty()) {
            "Missing required variables: ${missingRequired.joinToString(", ")}"
        }
    }

    fun resolve(text: String): String {
        val resolvedValues = values.mapValues { it.value ?: "" }
        return PLACEHOLDER_REGEX.replace(text) { match ->
            val key = match.groupValues[1]
            resolvedValues[key] ?: ""
        }
    }

    companion object {
        // ${variable} 형태의 플레이스홀더를 찾는다 (영문/숫자/./_/ - 허용).
        private val PLACEHOLDER_REGEX = Regex("\\$\\{([a-zA-Z0-9_.-]+)\\}")
        private val logger = LoggerFactory.getLogger(MailTemplateVariables::class.java)

        fun from(values: Map<String, String?>): MailTemplateVariables = MailTemplateVariables(values)
    }
}
