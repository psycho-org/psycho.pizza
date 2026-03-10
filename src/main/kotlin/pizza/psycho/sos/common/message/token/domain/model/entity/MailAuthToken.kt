package pizza.psycho.sos.common.message.token.domain.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseEntity
import pizza.psycho.sos.common.message.action.domain.MailActionType
import pizza.psycho.sos.common.message.domain.MessageChannel
import pizza.psycho.sos.common.message.domain.MessageType
import java.time.Instant

@Entity
@Table(name = "message_auth_tokens")
class MailAuthToken protected constructor(
    @Enumerated(EnumType.STRING)
    @Column(name = "mail_type", nullable = false, length = 50)
    var mailType: MessageType,
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    var channel: MessageChannel,
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    var actionType: MailActionType,
    @Column(name = "target_email", nullable = false, length = 255)
    var targetEmail: String,
    @Column(name = "context_key", length = 100)
    var contextKey: String? = null,
    @Column(name = "token", nullable = false, unique = true, length = 64)
    var token: String,
    @Column(name = "expired_at", nullable = false)
    var expiredAt: Instant,
    @Column(name = "verified_at")
    var verifiedAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 30)
    var failureReason: MailAuthFailureReason? = null,
    @Column(name = "failed_at")
    var failedAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "action_status", length = 20)
    var actionStatus: MailAuthActionStatus? = null,
    @Column(name = "action_error", columnDefinition = "TEXT")
    var actionError: String? = null,
    @Column(name = "action_processed_at")
    var actionProcessedAt: Instant? = null,
) : BaseEntity() {
    @OneToMany(
        mappedBy = "mailAuthToken",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    val params: MutableList<MailAuthTokenParam> = mutableListOf()

    fun isExpired(now: Instant = Instant.now()): Boolean = expiredAt.isBefore(now)

    fun isVerified(): Boolean = verifiedAt != null

    fun isFailed(): Boolean = failureReason != null

    fun verify(now: Instant = Instant.now()) {
        verifiedAt = now
    }

    fun fail(
        reason: MailAuthFailureReason,
        now: Instant = Instant.now(),
    ) {
        if (failureReason == null) {
            failureReason = reason
            failedAt = now
        }
    }

    fun markActionPending() {
        actionStatus = MailAuthActionStatus.PENDING
        actionError = null
        actionProcessedAt = null
    }

    fun markActionSuccess(now: Instant = Instant.now()) {
        actionStatus = MailAuthActionStatus.SUCCESS
        actionError = null
        actionProcessedAt = now
    }

    fun markActionFailed(
        error: String?,
        now: Instant = Instant.now(),
    ) {
        actionStatus = MailAuthActionStatus.FAILED
        actionError = error?.trim()?.takeIf { it.isNotEmpty() }?.take(1000)
        actionProcessedAt = now
    }

    fun addParams(values: Map<String, String?>) {
        values.forEach { (key, rawValue) ->
            val value = rawValue?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            params.add(MailAuthTokenParam.create(this, key, value))
        }
    }

    fun paramsMap(): Map<String, String?> = params.associate { it.name to it.value }

    companion object {
        fun issue(
            mailType: MessageType,
            channel: MessageChannel,
            actionType: MailActionType,
            targetEmail: String,
            contextKey: String? = null,
            token: String,
            expiredAt: Instant,
        ): MailAuthToken =
            MailAuthToken(
                mailType = mailType,
                channel = channel,
                actionType = actionType,
                targetEmail = targetEmail,
                contextKey = contextKey,
                token = token,
                expiredAt = expiredAt,
            )
    }
}
