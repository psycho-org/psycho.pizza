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
        const val MESSAGE = "StrongPassword must be ≥12 chars and include upper, lower, digit, special char"
        const val MESSAGE_KO = "비밀번호는 12자 이상이며, 대문자·소문자·숫자·특수문자를 모두 포함해야 합니다."
        private val REGEX = Regex(PATTERN)
    }
}
