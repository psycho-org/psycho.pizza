package pizza.psycho.sos.identity.security.token

import java.util.UUID

data class AccessTokenClaims(
    val accountId: UUID,
    val email: String,
)
