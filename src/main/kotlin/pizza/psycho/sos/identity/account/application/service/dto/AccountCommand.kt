package pizza.psycho.sos.identity.account.application.service.dto

import java.util.UUID

sealed interface AccountCommand {
    data class Register(
        val email: String,
        val password: String,
        val firstName: String,
        val lastName: String,
    ) : AccountCommand

    data class UpdateDisplayName(
        val accountId: UUID,
        val displayName: String,
    ) : AccountCommand
}
