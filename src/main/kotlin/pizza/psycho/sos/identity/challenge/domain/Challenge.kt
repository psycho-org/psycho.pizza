package pizza.psycho.sos.identity.challenge.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseEntity
import pizza.psycho.sos.identity.account.domain.vo.Email
import pizza.psycho.sos.identity.challenge.domain.vo.ChallengeStatus
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.time.Instant

@Entity
@Table(name = "challenges")
class Challenge protected constructor() : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    var operationType: OperationType = OperationType.REGISTER
        protected set

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "target_email", nullable = false))
    var targetEmail: Email = Email()
        protected set

    @Column(name = "otp_hash", nullable = false)
    var otpHash: String = ""
        protected set

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now()
        protected set

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0
        protected set

    @Column(name = "max_attempts", nullable = false)
    var maxAttempts: Int = 5
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ChallengeStatus = ChallengeStatus.PENDING
        protected set

    fun isExpired(now: Instant = Instant.now()): Boolean = expiresAt.isBefore(now) || expiresAt == now

    fun hasExceededMaxAttempts(): Boolean = attemptCount >= maxAttempts

    fun incrementAttempt() {
        attemptCount++
    }

    fun markVerified() {
        require(status == ChallengeStatus.PENDING) { "Only PENDING challenges can be verified" }
        status = ChallengeStatus.VERIFIED
    }

    fun markFailed() {
        require(status == ChallengeStatus.PENDING) { "Only PENDING challenges can be marked as failed" }
        status = ChallengeStatus.FAILED
    }

    fun markExpired() {
        require(status == ChallengeStatus.PENDING) { "Only PENDING challenges can be marked as expired" }
        status = ChallengeStatus.EXPIRED
    }

    companion object {
        fun create(
            operationType: OperationType,
            targetEmail: Email,
            otpHash: String,
            expiresAt: Instant,
            maxAttempts: Int = 5,
        ): Challenge =
            Challenge().apply {
                this.operationType = operationType
                this.targetEmail = targetEmail
                this.otpHash = otpHash
                this.expiresAt = expiresAt
                this.maxAttempts = maxAttempts
            }
    }
}
