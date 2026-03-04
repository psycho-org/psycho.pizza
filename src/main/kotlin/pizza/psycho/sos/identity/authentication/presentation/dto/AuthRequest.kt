package pizza.psycho.sos.identity.authentication.presentation.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

sealed interface AuthRequest {
    data class Login(
        @field:Email
        @field:NotBlank
        val email: String,
        @field:NotBlank
        val password: String,
    )
}
