package pizza.psycho.sos.identity.challenge.application.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.common.support.transaction.helper.hasConstraintName
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.identity.challenge.application.port.VerificationDelivery
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.application.service.dto.RequestChallengeResult
import pizza.psycho.sos.identity.challenge.application.service.dto.VerifyOtpResult
import pizza.psycho.sos.identity.challenge.config.ChallengeProperties
import pizza.psycho.sos.identity.challenge.domain.Challenge
import pizza.psycho.sos.identity.challenge.domain.ConfirmationToken
import pizza.psycho.sos.identity.challenge.domain.OtpGenerator
import pizza.psycho.sos.identity.challenge.domain.vo.ChallengeStatus
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import pizza.psycho.sos.identity.challenge.infrastructure.ChallengeRepository
import pizza.psycho.sos.identity.challenge.infrastructure.ConfirmationTokenRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class ChallengeService(
    private val accountRepository: AccountRepository,
    private val challengeRepository: ChallengeRepository,
    private val confirmationTokenRepository: ConfirmationTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val otpGenerator: OtpGenerator,
    private val verificationDelivery: VerificationDelivery,
    private val challengeProperties: ChallengeProperties,
) {
    fun createChallenge(command: ChallengeCommand.Request): RequestChallengeResult {
        val command = command.normalized()

        resolveExistingRegisterAccount(command)?.let { return it }

        return createChallengeHandlingConstraintRace(command)
    }

    @Transactional
    fun verifyOtp(command: ChallengeCommand.Verify): VerifyOtpResult {
        val challenge =
            challengeRepository.findByIdAndStatus(command.challengeId, ChallengeStatus.PENDING)
                ?: return VerifyOtpResult.Failure.ChallengeNotFound

        if (challenge.operationType != command.expectedOperationType) {
            return VerifyOtpResult.Failure.OperationTypeMismatch
        }

        if (!requesterEmailMatches(command.requesterEmail, challenge.targetEmail)) {
            return VerifyOtpResult.Failure.RequesterEmailMismatch
        }

        if (challenge.isExpired()) {
            challenge.markExpired()
            return VerifyOtpResult.Failure.ChallengeExpired
        }

        if (challenge.hasExceededMaxAttempts()) {
            challenge.markFailed()
            return VerifyOtpResult.Failure.MaxAttemptsExceeded
        }

        challenge.incrementAttempt()

        if (!passwordEncoder.matches(command.otpCode, challenge.otpHash)) {
            if (challenge.hasExceededMaxAttempts()) {
                challenge.markFailed()
            }
            return VerifyOtpResult.Failure.InvalidOtp
        }

        challenge.markVerified()

        val now = Instant.now()
        val token =
            ConfirmationToken.create(
                challenge = challenge,
                operationType = challenge.operationType,
                targetEmail = challenge.targetEmail,
                expiresAt = now.plusSeconds(challengeProperties.confirmationTokenTtlSeconds),
            )

        val savedToken = confirmationTokenRepository.save(token)

        return VerifyOtpResult.Success(
            confirmationTokenId = savedToken.id(),
            targetEmail = savedToken.targetEmail,
        )
    }

    @Transactional
    fun acquireUsableToken(command: ChallengeCommand.AcquireToken): ConfirmationToken? =
        confirmationTokenRepository.findUsableByIdAndOperationTypeForUpdate(
            id = command.tokenId,
            operationType = command.operationType,
            now = Instant.now(),
        )

    private fun createChallengeInTransaction(command: ChallengeCommand.Request): RequestChallengeResult {
        val now = Instant.now()
        val email = Email.of(command.email)

        return when (val decision = resolvePendingDecision(email, command.operationType, now)) {
            PendingDecision.Proceed -> issueChallenge(email, command.operationType, now)
            is PendingDecision.Cooldown -> cooldownActive(now, decision.availableAt)
        }
    }

    companion object {
        private const val PENDING_CHALLENGE_CONSTRAINT_NAME = "uk_challenges_email_op_pending"
    }

    private sealed interface PendingDecision {
        data object Proceed : PendingDecision

        data class Cooldown(
            val availableAt: Instant,
        ) : PendingDecision
    }

    private fun ChallengeCommand.Request.normalized(): ChallengeCommand.Request = copy(email = Email.of(email).value)

    private fun resolveExistingRegisterAccount(command: ChallengeCommand.Request): RequestChallengeResult.Success? {
        if (command.operationType != OperationType.REGISTER) {
            return null
        }

        accountRepository.findByEmailValueIgnoreCaseAndDeletedAtIsNull(command.email) ?: return null

        // TODO: When the mail delivery contract is ready, pass account summary data
        // (email, familyName, givenName, createdAt) here and request the existing-account notice mail.
        return RequestChallengeResult.Success(
            challengeId = UUID.randomUUID(),
            expiresAt = Instant.now().plusSeconds(challengeProperties.otpTtlSeconds),
        )
    }

    private fun createChallengeHandlingConstraintRace(command: ChallengeCommand.Request): RequestChallengeResult =
        try {
            Tx.writable { createChallengeInTransaction(command) }
        } catch (ex: RuntimeException) {
            if (ex.hasConstraintName(PENDING_CHALLENGE_CONSTRAINT_NAME)) {
                resolveCooldownActive(command)
            } else {
                throw ex
            }
        }

    private fun resolvePendingDecision(
        email: Email,
        operationType: OperationType,
        now: Instant,
    ): PendingDecision {
        val pending = findPendingChallenge(email, operationType) ?: return PendingDecision.Proceed
        val availableAt = pending.cooldownAvailableAt()

        if (availableAt.isAfter(now)) {
            return PendingDecision.Cooldown(availableAt)
        }

        pending.markExpired()
        challengeRepository.saveAndFlush(pending)

        return PendingDecision.Proceed
    }

    private fun issueChallenge(
        email: Email,
        operationType: OperationType,
        now: Instant,
    ): RequestChallengeResult.Success {
        val otp = otpGenerator.generate(challengeProperties.otpLength)
        val otpHash = passwordEncoder.encode(otp)

        val challenge =
            Challenge.create(
                operationType = operationType,
                targetEmail = email,
                otpHash = otpHash,
                expiresAt = now.plusSeconds(challengeProperties.otpTtlSeconds),
                maxAttempts = challengeProperties.otpMaxAttempts,
            )

        val saved = challengeRepository.saveAndFlush(challenge)

        verificationDelivery.sendOtp(email, otp, operationType)

        return RequestChallengeResult.Success(
            challengeId = requireNotNull(saved.id),
            expiresAt = saved.expiresAt,
        )
    }

    private fun resolveCooldownActive(command: ChallengeCommand.Request): RequestChallengeResult.Failure.CooldownActive {
        val now = Instant.now()
        val email = Email.of(command.email)
        val availableAt =
            findPendingChallenge(email, command.operationType)?.let { it.cooldownAvailableAt() }
                ?: now.plusSeconds(challengeProperties.cooldownSeconds)
        return cooldownActive(now, availableAt)
    }

    private fun findPendingChallenge(
        email: Email,
        operationType: OperationType,
    ): Challenge? =
        challengeRepository.findByTargetEmailValueIgnoreCaseAndOperationTypeAndStatus(
            targetEmail = email.value,
            operationType = operationType,
            status = ChallengeStatus.PENDING,
        )

    private fun Challenge.cooldownAvailableAt(): Instant = requireNotNull(createdAt).plusSeconds(challengeProperties.cooldownSeconds)

    private fun cooldownActive(
        now: Instant,
        availableAt: Instant,
    ): RequestChallengeResult.Failure.CooldownActive =
        RequestChallengeResult.Failure.CooldownActive(
            availableAt = availableAt,
            retryAfterSeconds = retryAfterSeconds(now, availableAt),
        )

    private fun retryAfterSeconds(
        now: Instant,
        availableAt: Instant,
    ): Long {
        if (!availableAt.isAfter(now)) {
            return 0
        }

        val millis = Duration.between(now, availableAt).toMillis()
        return maxOf(1, (millis + 999) / 1000)
    }

    private fun requesterEmailMatches(
        requesterEmail: String?,
        targetEmail: Email,
    ): Boolean {
        if (requesterEmail == null) {
            return true
        }

        return Email.of(requesterEmail) == targetEmail
    }
}
