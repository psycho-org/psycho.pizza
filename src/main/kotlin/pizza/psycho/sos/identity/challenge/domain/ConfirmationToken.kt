package pizza.psycho.sos.identity.challenge.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseEntity
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "confirmation_tokens")
class ConfirmationToken protected constructor() : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    var challenge: Challenge? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    var operationType: OperationType = OperationType.REGISTER
        protected set

    @Column(name = "target_email", nullable = false)
    var targetEmail: String = ""
        protected set

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now()
        protected set

    @Column(name = "used", nullable = false)
    var used: Boolean = false
        protected set

    fun isExpired(now: Instant = Instant.now()): Boolean = expiresAt.isBefore(now) || expiresAt == now

    fun consume() {
        require(!used) { "Token has already been consumed" }
        used = true
    }

    fun id(): UUID = requireNotNull(id) { "ConfirmationToken.id must not be null after persistence" }

    companion object {
        fun create(
            challenge: Challenge,
            operationType: OperationType,
            targetEmail: String,
            expiresAt: Instant,
        ): ConfirmationToken =
            ConfirmationToken().apply {
                this.challenge = challenge
                this.operationType = operationType
                this.targetEmail = targetEmail.lowercase()
                this.expiresAt = expiresAt
            }
    }
}
