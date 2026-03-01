package pizza.psycho.sos.identity.account.presentation.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

sealed interface AccountRequest {
    data class Register(
        @field:Email
        @field:NotBlank
        val email: String,
        @field:NotBlank
        @field:Size(min = 8, max = 36)
        val password: String,
        @field:NotBlank
        val firstName: String,
        @field:NotBlank
        val lastName: String,
    )

    data class Withdraw(
        // TODO
        val example: Nothing,
    )
}
