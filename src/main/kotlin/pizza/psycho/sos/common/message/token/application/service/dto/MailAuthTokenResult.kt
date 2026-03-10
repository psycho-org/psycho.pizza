package pizza.psycho.sos.common.message.token.application.service.dto

import pizza.psycho.sos.common.message.token.domain.model.entity.MailAuthToken

sealed interface MailAuthTokenResult {
    data class Verified(
        val token: MailAuthToken,
    ) : MailAuthTokenResult

    data class AlreadyVerified(
        val token: MailAuthToken,
    ) : MailAuthTokenResult

    data class Expired(
        val token: MailAuthToken,
    ) : MailAuthTokenResult

    data class Failed(
        val token: MailAuthToken,
    ) : MailAuthTokenResult

    data object NotFound : MailAuthTokenResult
}
