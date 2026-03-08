package pizza.psycho.sos.identity.account.presentation.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import pizza.psycho.sos.identity.account.domain.policy.StrongPassword

sealed interface AccountRequest {
    data class Register(
        @field:Email
        @field:NotBlank
        val email: String,
        @field:NotBlank
        @field:Size(min = 12, max = 64)
        @field:StrongPassword
        val password: String,
        @field:NotBlank
        val givenName: String,
        @field:NotBlank
        val familyName: String,
    )

    sealed interface Update : AccountRequest {
        data class DisplayName(
            @field:NotBlank
            val displayName: String,
        ) : Update

        data class Password(
            @field:NotBlank
            @field:Size(min = 12, max = 64)
            @field:StrongPassword
            val password: String,
        ) : Update

        data class Name(
            @field:NotBlank
            val givenName: String,
            @field:NotBlank
            val familyName: String,
        ) : Update
    }

    data class Withdraw(
        @field:NotBlank
        val password: String,
    )
}
