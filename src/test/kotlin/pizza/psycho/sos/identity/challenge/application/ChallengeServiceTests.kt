package pizza.psycho.sos.identity.challenge.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.identity.challenge.application.port.VerificationDelivery
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.application.service.dto.ConsumeTokenResult
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
import java.time.Instant
import java.util.UUID

class ChallengeServiceTests {
    private val challengeRepository = mock(ChallengeRepository::class.java)
    private val confirmationTokenRepository = mock(ConfirmationTokenRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val otpGenerator = mock(OtpGenerator::class.java)
    private val verificationDelivery = mock(VerificationDelivery::class.java)
    private val challengeProperties =
        ChallengeProperties().apply {
            otpLength = 6
            otpTtlSeconds = 300
            otpMaxAttempts = 3
            cooldownSeconds = 60
            confirmationTokenTtlSeconds = 300
        }
    private val challengeService =
        ChallengeService(
            challengeRepository = challengeRepository,
            confirmationTokenRepository = confirmationTokenRepository,
            passwordEncoder = passwordEncoder,
            otpGenerator = otpGenerator,
            verificationDelivery = verificationDelivery,
            challengeProperties = challengeProperties,
        )

    @Test
    fun `createChallenge returns cooldown active when existing pending is still within cooldown`() {
        val pending =
            Challenge
                .create(
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "old-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ).also { it.createdAt = Instant.now() }
        `when`(
            challengeRepository.findByTargetEmailValueIgnoreCaseAndOperationTypeAndStatus(
                "user@psycho.pizza",
                OperationType.REGISTER,
                ChallengeStatus.PENDING,
            ),
        ).thenReturn(pending)

        val result =
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "user@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            )

        assertEquals(RequestChallengeResult.Failure.CooldownActive, result)
        verify(challengeRepository, never()).save(any(Challenge::class.java))
        verifyNoInteractions(verificationDelivery)
    }

    @Test
    fun `createChallenge expires old pending challenge and issues a new one when cooldown has passed`() {
        val pending =
            Challenge
                .create(
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "old-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ).also {
                    it.id = UUID.fromString("00000000-0000-0000-0000-000000000001")
                    it.createdAt = Instant.now().minusSeconds(120)
                }
        val savedNewChallengeId = UUID.fromString("00000000-0000-0000-0000-000000000002")
        `when`(
            challengeRepository.findByTargetEmailValueIgnoreCaseAndOperationTypeAndStatus(
                "user@psycho.pizza",
                OperationType.REGISTER,
                ChallengeStatus.PENDING,
            ),
        ).thenReturn(pending)
        `when`(otpGenerator.generate(6)).thenReturn("123456")
        `when`(passwordEncoder.encode("123456")).thenReturn("new-hash")
        `when`(challengeRepository.save(any(Challenge::class.java))).thenAnswer { invocation ->
            invocation.getArgument<Challenge>(0).also { it.id = savedNewChallengeId }
        }

        val result =
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "user@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            )

        assertTrue(result is RequestChallengeResult.Success)
        assertEquals(savedNewChallengeId, (result as RequestChallengeResult.Success).challengeId)
        assertEquals(ChallengeStatus.EXPIRED, pending.status)
        verify(challengeRepository).saveAndFlush(pending)
        verify(verificationDelivery).sendOtp(Email.of("user@psycho.pizza"), "123456", OperationType.REGISTER)
    }

    @Test
    fun `verifyOtp returns challenge not found when pending challenge is missing`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000010")
        `when`(challengeRepository.findByIdAndStatus(challengeId, ChallengeStatus.PENDING)).thenReturn(null)

        val result =
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "123456",
                    expectedOperationType = OperationType.REGISTER,
                    requesterEmail = null,
                ),
            )

        assertEquals(VerifyOtpResult.Failure.ChallengeNotFound, result)
    }

    @Test
    fun `verifyOtp returns invalid otp and increments attempt count when otp does not match`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val challenge =
            Challenge
                .create(
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "otp-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ).also { it.id = challengeId }
        `when`(challengeRepository.findByIdAndStatus(challengeId, ChallengeStatus.PENDING)).thenReturn(challenge)
        `when`(passwordEncoder.matches("000000", "otp-hash")).thenReturn(false)

        val result =
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "000000",
                    expectedOperationType = OperationType.REGISTER,
                    requesterEmail = null,
                ),
            )

        assertEquals(VerifyOtpResult.Failure.InvalidOtp, result)
        assertEquals(1, challenge.attemptCount)
        assertEquals(ChallengeStatus.PENDING, challenge.status)
    }

    @Test
    fun `verifyOtp returns operation type mismatch when challenge operation differs from expected`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000012")
        val challenge =
            Challenge
                .create(
                    operationType = OperationType.WITHDRAW,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "otp-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ).also { it.id = challengeId }
        `when`(challengeRepository.findByIdAndStatus(challengeId, ChallengeStatus.PENDING)).thenReturn(challenge)

        val result =
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "123456",
                    expectedOperationType = OperationType.REGISTER,
                    requesterEmail = null,
                ),
            )

        assertEquals(VerifyOtpResult.Failure.OperationTypeMismatch, result)
        assertEquals(0, challenge.attemptCount)
        assertEquals(ChallengeStatus.PENDING, challenge.status)
        verifyNoInteractions(confirmationTokenRepository)
    }

    @Test
    fun `verifyOtp returns requester email mismatch when authenticated requester differs from challenge target`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000013")
        val challenge =
            Challenge
                .create(
                    operationType = OperationType.CHANGE_PASSWORD,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "otp-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ).also { it.id = challengeId }
        `when`(challengeRepository.findByIdAndStatus(challengeId, ChallengeStatus.PENDING)).thenReturn(challenge)

        val result =
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "123456",
                    expectedOperationType = OperationType.CHANGE_PASSWORD,
                    requesterEmail = "other@psycho.pizza",
                ),
            )

        assertEquals(VerifyOtpResult.Failure.RequesterEmailMismatch, result)
        assertEquals(0, challenge.attemptCount)
        assertEquals(ChallengeStatus.PENDING, challenge.status)
        verifyNoInteractions(confirmationTokenRepository)
    }

    @Test
    fun `verifyOtp returns confirmation token on success when operation and requester match`() {
        val challengeId = UUID.fromString("00000000-0000-0000-0000-000000000012")
        val tokenId = UUID.fromString("00000000-0000-0000-0000-000000000014")
        val challenge =
            Challenge
                .create(
                    operationType = OperationType.CHANGE_PASSWORD,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "otp-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ).also { it.id = challengeId }
        `when`(challengeRepository.findByIdAndStatus(challengeId, ChallengeStatus.PENDING)).thenReturn(challenge)
        `when`(passwordEncoder.matches("123456", "otp-hash")).thenReturn(true)
        `when`(confirmationTokenRepository.save(any(ConfirmationToken::class.java))).thenAnswer { invocation ->
            invocation.getArgument<ConfirmationToken>(0).also { it.id = tokenId }
        }

        val result =
            challengeService.verifyOtp(
                ChallengeCommand.Verify(
                    challengeId = challengeId,
                    otpCode = "123456",
                    expectedOperationType = OperationType.CHANGE_PASSWORD,
                    requesterEmail = "user@psycho.pizza",
                ),
            )

        assertTrue(result is VerifyOtpResult.Success)
        val success = result as VerifyOtpResult.Success
        assertEquals(tokenId, success.confirmationTokenId)
        assertEquals(Email.of("user@psycho.pizza"), success.targetEmail)
        assertEquals(ChallengeStatus.VERIFIED, challenge.status)
    }

    @Test
    fun `consumeToken rejects operation type mismatch`() {
        val tokenId = UUID.fromString("00000000-0000-0000-0000-000000000020")
        val challenge =
            Challenge.create(
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("user@psycho.pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )
        val token =
            ConfirmationToken
                .create(
                    challenge = challenge,
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
                    expiresAt = Instant.now().plusSeconds(300),
                ).also { it.id = tokenId }
        `when`(confirmationTokenRepository.findByIdAndUsedFalse(tokenId)).thenReturn(token)

        val result =
            challengeService.consumeToken(
                ChallengeCommand.ConsumeToken(
                    tokenId = tokenId,
                    operationType = OperationType.WITHDRAW,
                ),
            )

        assertEquals(ConsumeTokenResult.Failure.OperationTypeMismatch, result)
        assertTrue(!token.used)
    }

    @Test
    fun `consumeToken marks token used and returns target email on success`() {
        val tokenId = UUID.fromString("00000000-0000-0000-0000-000000000021")
        val challenge =
            Challenge.create(
                operationType = OperationType.CHANGE_PASSWORD,
                targetEmail = Email.of("user@psycho.pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )
        val token =
            ConfirmationToken
                .create(
                    challenge = challenge,
                    operationType = OperationType.CHANGE_PASSWORD,
                    targetEmail = Email.of("user@psycho.pizza"),
                    expiresAt = Instant.now().plusSeconds(300),
                ).also { it.id = tokenId }
        `when`(confirmationTokenRepository.findByIdAndUsedFalse(tokenId)).thenReturn(token)

        val result =
            challengeService.consumeToken(
                ChallengeCommand.ConsumeToken(
                    tokenId = tokenId,
                    operationType = OperationType.CHANGE_PASSWORD,
                ),
            )

        assertTrue(result is ConsumeTokenResult.Success)
        assertEquals(Email.of("user@psycho.pizza"), (result as ConsumeTokenResult.Success).targetEmail)
        assertTrue(token.used)
    }
}
