package pizza.psycho.sos.identity.authentication.application.security

import java.util.UUID

data class AuthenticatedAccountPrincipal(
    val accountId: UUID,
    val email: String,
)
