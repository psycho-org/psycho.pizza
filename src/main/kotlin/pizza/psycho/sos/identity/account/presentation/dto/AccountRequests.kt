package pizza.psycho.sos.identity.account.presentation.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
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

data class WithdrawRequest(
    // TODO
    val example: Nothing,
)
