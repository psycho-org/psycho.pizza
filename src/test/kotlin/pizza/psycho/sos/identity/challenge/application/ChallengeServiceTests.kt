package pizza.psycho.sos.identity.challenge.application

import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.common.support.transaction.runner.TransactionRunner
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.identity.challenge.application.port.VerificationDelivery
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
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
import java.time.Instant
import java.util.UUID

class ChallengeServiceTests {
    private val accountRepository = mock(AccountRepository::class.java)
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
            accountRepository = accountRepository,
            challengeRepository = challengeRepository,
            confirmationTokenRepository = confirmationTokenRepository,
            passwordEncoder = passwordEncoder,
            otpGenerator = otpGenerator,
            verificationDelivery = verificationDelivery,
            challengeProperties = challengeProperties,
        )

    @BeforeEach
    fun setUp() {
        Tx.initialize(TransactionRunner())
    }

    @Test
    fun `createChallenge returns synthetic challenge when active account already exists for register`() {
        val existingAccount =
            Account.create(
                email = Email.of("existing@psycho.pizza"),
                passwordHash = "hash",
                givenName = "given",
                familyName = "family",
            )
        `when`(accountRepository.findByEmailValueIgnoreCaseAndDeletedAtIsNull("existing@psycho.pizza")).thenReturn(existingAccount)

        val before = Instant.now()
        val result =
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "Existing@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            )
        val after = Instant.now()

        assertTrue(result is RequestChallengeResult.Success)
        val success = result as RequestChallengeResult.Success
        assertTrue(!success.expiresAt.isBefore(before.plusSeconds(challengeProperties.otpTtlSeconds)))
        assertTrue(!success.expiresAt.isAfter(after.plusSeconds(challengeProperties.otpTtlSeconds)))
        verify(accountRepository).findByEmailValueIgnoreCaseAndDeletedAtIsNull("existing@psycho.pizza")
        verifyNoInteractions(challengeRepository, verificationDelivery)
    }

    @Test
    fun `createChallenge skips account lookup for non-register operations`() {
        `when`(
            challengeRepository.findByTargetEmailValueIgnoreCaseAndOperationTypeAndStatus(
                "me@psycho.pizza",
                OperationType.WITHDRAW,
                ChallengeStatus.PENDING,
            ),
        ).thenReturn(null)
        `when`(otpGenerator.generate(6)).thenReturn("123456")
        `when`(passwordEncoder.encode("123456")).thenReturn("new-hash")
        `when`(challengeRepository.saveAndFlush(any(Challenge::class.java))).thenAnswer { invocation ->
            invocation.getArgument<Challenge>(0).also { challenge ->
                if (challenge.id == null) {
                    challenge.id = UUID.fromString("00000000-0000-0000-0000-000000000099")
                }
            }
        }

        val result =
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "me@psycho.pizza",
                    operationType = OperationType.WITHDRAW,
                ),
            )

        assertTrue(result is RequestChallengeResult.Success)
        verifyNoInteractions(accountRepository)
    }

    @Test
    fun `createChallenge returns cooldown active when existing pending is still within cooldown`() {
        val createdAt = Instant.now()
        val pending =
            Challenge
                .create(
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "old-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ).also { it.createdAt = createdAt }
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

        assertTrue(result is RequestChallengeResult.Failure.CooldownActive)
        val failure = result as RequestChallengeResult.Failure.CooldownActive
        assertEquals(createdAt.plusSeconds(challengeProperties.cooldownSeconds), failure.availableAt)
        assertTrue(failure.retryAfterSeconds in 1..challengeProperties.cooldownSeconds)
        verify(accountRepository).findByEmailValueIgnoreCaseAndDeletedAtIsNull("user@psycho.pizza")
        verify(challengeRepository, never()).saveAndFlush(any(Challenge::class.java))
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
        var issuedChallenge: Challenge? = null
        `when`(challengeRepository.saveAndFlush(any(Challenge::class.java))).thenAnswer { invocation ->
            invocation.getArgument<Challenge>(0).also {
                if (it.status == ChallengeStatus.PENDING && it.id == null) {
                    issuedChallenge = it
                    it.id = savedNewChallengeId
                }
            }
        }

        val result =
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "user@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            )

        assertTrue(result is RequestChallengeResult.Success)
        val success = result as RequestChallengeResult.Success
        assertEquals(savedNewChallengeId, success.challengeId)
        assertEquals(requireNotNull(issuedChallenge).expiresAt, success.expiresAt)
        assertEquals(ChallengeStatus.EXPIRED, pending.status)
        verify(challengeRepository, times(2)).saveAndFlush(any(Challenge::class.java))
        verify(verificationDelivery).sendOtp(Email.of("user@psycho.pizza"), "123456", OperationType.REGISTER)
    }

    @Test
    fun `createChallenge maps pending challenge unique constraint to cooldown active`() {
        val createdAt = Instant.now().minusSeconds(1)
        val pending =
            Challenge
                .create(
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "old-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ).also { it.createdAt = createdAt }
        `when`(
            challengeRepository.findByTargetEmailValueIgnoreCaseAndOperationTypeAndStatus(
                "user@psycho.pizza",
                OperationType.REGISTER,
                ChallengeStatus.PENDING,
            ),
        ).thenReturn(null, pending)
        `when`(otpGenerator.generate(6)).thenReturn("123456")
        `when`(passwordEncoder.encode("123456")).thenReturn("new-hash")
        `when`(challengeRepository.saveAndFlush(any(Challenge::class.java))).thenThrow(
            DataIntegrityViolationException(
                "duplicate pending challenge",
                ConstraintViolationException(
                    "duplicate pending challenge",
                    java.sql.SQLException("duplicate pending challenge"),
                    "uk_challenges_email_op_pending",
                ),
            ),
        )

        val result =
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "user@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            )

        assertTrue(result is RequestChallengeResult.Failure.CooldownActive)
        val failure = result as RequestChallengeResult.Failure.CooldownActive
        assertEquals(createdAt.plusSeconds(challengeProperties.cooldownSeconds), failure.availableAt)
        assertTrue(failure.retryAfterSeconds in 1..challengeProperties.cooldownSeconds)
        verifyNoInteractions(verificationDelivery)
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
    fun `acquireUsableToken returns null when repository cannot find a usable token`() {
        val tokenId = UUID.fromString("00000000-0000-0000-0000-000000000020")
        `when`(
            confirmationTokenRepository.findUsableByIdAndOperationTypeForUpdate(
                eqNonNull(tokenId),
                eqNonNull(OperationType.WITHDRAW),
                anyNonNull(),
            ),
        ).thenReturn(null)

        val result =
            challengeService.acquireUsableToken(
                ChallengeCommand.AcquireToken(
                    tokenId = tokenId,
                    operationType = OperationType.WITHDRAW,
                ),
            )

        assertEquals(null, result)
    }

    @Test
    fun `acquireUsableToken returns locked token without consuming it`() {
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
        `when`(
            confirmationTokenRepository.findUsableByIdAndOperationTypeForUpdate(
                eqNonNull(tokenId),
                eqNonNull(OperationType.CHANGE_PASSWORD),
                anyNonNull(),
            ),
        ).thenReturn(token)

        val result =
            challengeService.acquireUsableToken(
                ChallengeCommand.AcquireToken(
                    tokenId = tokenId,
                    operationType = OperationType.CHANGE_PASSWORD,
                ),
            )

        assertEquals(token, result)
        assertTrue(!token.used)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        any<T>()
        return null as T
    }

    private fun <T> eqNonNull(value: T): T {
        org.mockito.Mockito.eq(value)
        return value
    }
}
