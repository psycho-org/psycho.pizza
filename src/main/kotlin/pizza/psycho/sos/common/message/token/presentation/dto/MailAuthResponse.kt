package pizza.psycho.sos.common.message.token.presentation.dto

import java.time.Instant

data class MailAuthResponse(
    val status: String,
    val mailType: String? = null,
    val expiredAt: Instant? = null,
    val verifiedAt: Instant? = null,
)
