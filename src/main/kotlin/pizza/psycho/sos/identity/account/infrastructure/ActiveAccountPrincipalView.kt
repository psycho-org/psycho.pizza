package pizza.psycho.sos.identity.account.infrastructure

import java.util.UUID

data class ActiveAccountPrincipalView(
    val accountId: UUID,
    val email: String,
)
