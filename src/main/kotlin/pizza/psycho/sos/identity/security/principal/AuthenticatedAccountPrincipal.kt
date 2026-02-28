package pizza.psycho.sos.identity.security.principal

import java.util.UUID

data class AuthenticatedAccountPrincipal(
    val accountId: UUID,
    val email: String,
)
