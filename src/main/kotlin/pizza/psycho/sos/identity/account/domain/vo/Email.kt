package pizza.psycho.sos.identity.account.domain.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import pizza.psycho.sos.identity.account.domain.exception.InvalidEmailException

@Embeddable
data class Email(
    @Column(name = "email", nullable = false)
    val value: String = "",
) {
    companion object {
        private val EMAIL_PATTERN =
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun of(raw: String): Email {
            val normalized = raw.trim().lowercase()
            if (normalized.isBlank()) {
                throw InvalidEmailException("email must not be blank")
            }
            if (!EMAIL_PATTERN.matches(normalized)) {
                throw InvalidEmailException("invalid email format")
            }
            return Email(normalized)
        }
    }
}
