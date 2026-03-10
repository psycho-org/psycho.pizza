package pizza.psycho.sos.common.message.token.application.service

import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.message.action.application.model.MailActionRequest
import pizza.psycho.sos.common.message.action.application.model.WorkspaceInviteActionParams
import pizza.psycho.sos.common.message.action.domain.MailActionType
import pizza.psycho.sos.common.message.domain.MessageChannel
import pizza.psycho.sos.common.message.domain.MessageType
import pizza.psycho.sos.common.message.token.application.event.MailAuthVerifiedEvent
import pizza.psycho.sos.common.message.token.application.service.dto.MailAuthTokenResult
import pizza.psycho.sos.common.message.token.domain.model.entity.MailAuthFailureReason
import pizza.psycho.sos.common.message.token.domain.model.entity.MailAuthToken
import pizza.psycho.sos.common.message.token.domain.repository.MailAuthTokenRepository
import java.security.SecureRandom
import java.time.Instant

@Service
class MailAuthTokenService(
    private val mailAuthTokenRepository: MailAuthTokenRepository,
    private val entityManager: EntityManager,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun issue(
        mailType: MessageType,
        channel: MessageChannel,
        actionType: MailActionType,
        targetEmail: String,
        expiredAt: Instant,
        params: Map<String, String?> = emptyMap(),
        contextKey: String? = null,
    ): MailAuthToken {
        val normalizedEmail = targetEmail.trim().lowercase()
        supersedePending(mailType, normalizedEmail, contextKey)
        val token = generateToken()
        val entity = MailAuthToken.issue(mailType, channel, actionType, normalizedEmail, contextKey, token, expiredAt)
        if (params.isNotEmpty()) {
            entity.addParams(params)
        }
        return try {
            mailAuthTokenRepository.save(entity)
        } catch (ex: DataIntegrityViolationException) {
            supersedePending(mailType, normalizedEmail, contextKey)
            val retry =
                MailAuthToken.issue(
                    mailType,
                    channel,
                    actionType,
                    normalizedEmail,
                    contextKey,
                    generateToken(),
                    expiredAt,
                )
            if (params.isNotEmpty()) {
                retry.addParams(params)
            }
            try {
                mailAuthTokenRepository.save(retry)
            } catch (retryEx: DataIntegrityViolationException) {
                throw DomainException("duplicate pending mail auth token", retryEx)
            }
        }
    }

    @Transactional
    fun verify(token: String): MailAuthTokenResult {
        val entity = mailAuthTokenRepository.findByTokenWithParams(token) ?: return MailAuthTokenResult.NotFound

        if (entity.isVerified()) {
            return MailAuthTokenResult.AlreadyVerified(entity)
        }

        if (entity.isFailed()) {
            return MailAuthTokenResult.Failed(entity)
        }

        if (entity.isExpired()) {
            entity.fail(MailAuthFailureReason.EXPIRED)
            return MailAuthTokenResult.Expired(entity)
        }

        val actionRequest = toActionRequest(entity.actionType, entity.paramsMap())
        entity.verify()
        entity.markActionPending()
        eventPublisher.publishEvent(
            MailAuthVerifiedEvent(
                mailType = entity.mailType,
                token = entity.token,
                actionRequest = actionRequest,
            ),
        )
        return MailAuthTokenResult.Verified(entity)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val secureRandom = SecureRandom()
    }

    private fun toActionRequest(
        actionType: MailActionType,
        params: Map<String, String?>,
    ): MailActionRequest =
        when (actionType) {
            MailActionType.WORKSPACE_INVITE_ACCEPT ->
                MailActionRequest.WorkspaceInviteAccept(
                    params = WorkspaceInviteActionParams.from(params),
                )
        }

    private fun supersedePending(
        mailType: MessageType,
        targetEmail: String,
        contextKey: String?,
    ) {
        val pendingTokens =
            mailAuthTokenRepository.findPendingByTarget(
                mailType = mailType,
                targetEmail = targetEmail,
                contextKey = contextKey,
            )
        if (pendingTokens.isEmpty()) {
            return
        }
        pendingTokens.forEach { it.fail(MailAuthFailureReason.SUPERSEDED) }
        pendingTokens.forEach { mailAuthTokenRepository.save(it) }
        entityManager.flush()
    }
}
