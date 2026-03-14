package pizza.psycho.sos.identity.account.domain.policy

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class PasswordValidator : ConstraintValidator<StrongPassword, String> {
    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean = value?.let(REGEX::matches) ?: false

    companion object {
        const val MIN_LENGTH = 12
        const val MAX_LENGTH = 64
        const val PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{$MIN_LENGTH,$MAX_LENGTH}$"
        private val REGEX = Regex(PATTERN)
    }
}
