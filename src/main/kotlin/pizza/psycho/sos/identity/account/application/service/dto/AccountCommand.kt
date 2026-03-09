package pizza.psycho.sos.identity.account.application.service.dto

import java.util.UUID

sealed interface AccountCommand {
    data class Register(
        val confirmationTokenId: UUID,
        val email: String,
        val password: String,
        val firstName: String,
        val lastName: String,
    ) : AccountCommand

    sealed interface Update {
        data class DisplayName(
            val accountId: UUID,
            val displayName: String,
        ) : Update

        data class Name(
            val accountId: UUID,
            val givenName: String,
            val familyName: String,
        ) : Update

        data class Password(
            val accountId: UUID,
            val confirmationTokenId: UUID,
            val currentPassword: String,
            val newPassword: String,
        ) : Update
    }

    data class Withdraw(
        val accountId: UUID,
        val confirmationTokenId: UUID,
        val password: String,
    ) : AccountCommand
}
