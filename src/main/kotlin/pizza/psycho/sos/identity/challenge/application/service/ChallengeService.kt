package pizza.psycho.sos.identity.challenge.application.service

import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pizza.psycho.sos.identity.account.domain.vo.Email
import pizza.psycho.sos.identity.challenge.application.port.VerificationDelivery
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.application.service.dto.RequestChallengeResult
import pizza.psycho.sos.identity.challenge.application.service.dto.VerifyOtpResult
import pizza.psycho.sos.identity.challenge.config.ChallengeProperties
import pizza.psycho.sos.identity.challenge.domain.Challenge
import pizza.psycho.sos.identity.challenge.domain.ConfirmationToken
import pizza.psycho.sos.identity.challenge.domain.OtpGenerator
import pizza.psycho.sos.identity.challenge.domain.vo.ChallengeStatus
import pizza.psycho.sos.identity.challenge.infrastructure.ChallengeRepository
import pizza.psycho.sos.identity.challenge.infrastructure.ConfirmationTokenRepository
import java.time.Instant

@Service
@Transactional
class ChallengeService(
    private val challengeRepository: ChallengeRepository,
    private val confirmationTokenRepository: ConfirmationTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val otpGenerator: OtpGenerator,
    private val verificationDelivery: VerificationDelivery,
    private val challengeProperties: ChallengeProperties,
) {
    fun createChallenge(command: ChallengeCommand.Request): RequestChallengeResult {
        val now = Instant.now()
        val email = Email.of(command.email)

        challengeRepository
            .findByTargetEmailValueIgnoreCaseAndOperationTypeAndStatus(
                targetEmail = email.value,
                operationType = command.operationType,
                status = ChallengeStatus.PENDING,
            )?.let {
                if (requireNotNull(it.createdAt).plusSeconds(challengeProperties.cooldownSeconds).isAfter(now)) {
                    return RequestChallengeResult.Failure.CooldownActive
                }
                it.markExpired()
                challengeRepository.saveAndFlush(it)
            }

        val otp = otpGenerator.generate(challengeProperties.otpLength)
        val otpHash = passwordEncoder.encode(otp)

        val challenge =
            Challenge.create(
                operationType = command.operationType,
                targetEmail = email,
                otpHash = otpHash,
                expiresAt = now.plusSeconds(challengeProperties.otpTtlSeconds),
                maxAttempts = challengeProperties.otpMaxAttempts,
            )

        val saved = challengeRepository.save(challenge)

        verificationDelivery.sendOtp(email, otp, command.operationType)

        return RequestChallengeResult.Success(
            challengeId = requireNotNull(saved.id),
        )
    }

    fun verifyOtp(command: ChallengeCommand.Verify): VerifyOtpResult {
        val challenge =
            challengeRepository.findByIdAndStatus(command.challengeId, ChallengeStatus.PENDING)
                ?: return VerifyOtpResult.Failure.ChallengeNotFound

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

    fun acquireUsableToken(command: ChallengeCommand.AcquireToken): ConfirmationToken? =
        confirmationTokenRepository.findUsableByIdAndOperationTypeForUpdate(
            id = command.tokenId,
            operationType = command.operationType,
            now = Instant.now(),
        )
}
