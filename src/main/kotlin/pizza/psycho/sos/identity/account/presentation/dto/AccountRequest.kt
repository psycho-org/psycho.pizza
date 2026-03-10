package pizza.psycho.sos.identity.account.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pizza.psycho.sos.identity.account.domain.policy.StrongPassword
import java.util.UUID

sealed interface AccountRequest {
    data class Register(
        @field:NotNull
        var confirmationTokenId: UUID,
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

        data class Name(
            @field:NotBlank
            val givenName: String,
            @field:NotBlank
            val familyName: String,
        ) : Update

        data class Password(
            @field:NotNull
            var confirmationTokenId: UUID,
            @field:NotBlank
            val currentPassword: String,
            @field:NotBlank
            @field:Size(min = 12, max = 64)
            @field:StrongPassword
            val newPassword: String,
        ) : Update
    }

    data class Withdraw(
        @field:NotNull
        var confirmationTokenId: UUID,
        @field:NotBlank
        val password: String,
    )
}
