package pizza.psycho.sos.common.message.token.domain.repository

import pizza.psycho.sos.common.message.domain.MessageType
import pizza.psycho.sos.common.message.token.domain.model.entity.MailAuthToken

interface MailAuthTokenRepository {
    fun findByToken(token: String): MailAuthToken?

    fun findByTokenWithParams(token: String): MailAuthToken?

    fun findPendingByTarget(
        mailType: MessageType,
        targetEmail: String,
        contextKey: String?,
    ): List<MailAuthToken>

    fun save(token: MailAuthToken): MailAuthToken
}
