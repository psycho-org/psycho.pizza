package pizza.psycho.sos.common.domain.vo

import jakarta.persistence.Embeddable
import pizza.psycho.sos.common.exception.CommonErrorCode
import pizza.psycho.sos.common.handler.DomainException

@Embeddable
data class Email(
    val value: String = "",
) {
    companion object {
        private val EMAIL_PATTERN =
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun of(raw: String): Email {
            val normalized = raw.trim().lowercase()
            if (normalized.isBlank()) {
                throw DomainException(CommonErrorCode.COMMON_INVALID_EMAIL, "email must not be blank")
            }
            if (!EMAIL_PATTERN.matches(normalized)) {
                throw DomainException(CommonErrorCode.COMMON_INVALID_EMAIL, "invalid email format")
            }
            return Email(normalized)
        }
    }
}
