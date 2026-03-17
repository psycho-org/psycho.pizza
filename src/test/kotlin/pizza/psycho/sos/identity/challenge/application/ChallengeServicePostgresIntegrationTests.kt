package pizza.psycho.sos.identity.challenge.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Testcontainers
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.identity.challenge.application.port.VerificationDelivery
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.application.service.dto.RequestChallengeResult
import pizza.psycho.sos.identity.challenge.domain.Challenge
import pizza.psycho.sos.identity.challenge.domain.OtpGenerator
import pizza.psycho.sos.identity.challenge.domain.vo.ChallengeStatus
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import pizza.psycho.sos.identity.challenge.infrastructure.ChallengeRepository
import pizza.psycho.sos.identity.challenge.infrastructure.ConfirmationTokenRepository
import pizza.psycho.sos.identity.challenge.support.PostgresTestContainerSupport
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("tc")
@Testcontainers
@SpringBootTest
@ActiveProfiles("test-tc")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChallengeServicePostgresIntegrationTests : PostgresTestContainerSupport() {
    @Autowired
    private lateinit var challengeService: ChallengeService

    @Autowired
    private lateinit var challengeRepository: ChallengeRepository

    @Autowired
    private lateinit var confirmationTokenRepository: ConfirmationTokenRepository

    @MockitoBean
    private lateinit var otpGenerator: OtpGenerator

    @MockitoBean
    private lateinit var passwordEncoder: PasswordEncoder

    @MockitoBean
    private lateinit var verificationDelivery: VerificationDelivery

    @BeforeEach
    fun setUpMocks() {
        `when`(otpGenerator.generate(6)).thenReturn("123456")
        `when`(passwordEncoder.encode("123456")).thenReturn("otp-hash")
    }

    @Test
    fun `createChallenge rotates existing pending challenge without unique constraint failure`() {
        val existing =
            challengeRepository.saveAndFlush(
                Challenge.create(
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "old-hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 3,
                ),
            )
        val existingId = requireNotNull(existing.id)

        val result =
            challengeService.createChallenge(
                ChallengeCommand.Request(
                    email = "user@psycho.pizza",
                    operationType = OperationType.REGISTER,
                ),
            )

        assertTrue(result is RequestChallengeResult.Success)
        val newChallengeId = (result as RequestChallengeResult.Success).challengeId
        assertNotEquals(existingId, newChallengeId)

        val reloadedExisting =
            challengeRepository.findById(existingId).orElseThrow {
                IllegalStateException("Existing challenge should still exist")
            }
        assertEquals(ChallengeStatus.EXPIRED, reloadedExisting.status)

        val pending =
            challengeRepository.findByTargetEmailValueIgnoreCaseAndOperationTypeAndStatus(
                "user@psycho.pizza",
                OperationType.REGISTER,
                ChallengeStatus.PENDING,
            )
        assertEquals(newChallengeId, requireNotNull(pending).id)
        assertEquals(pending.expiresAt, (result as RequestChallengeResult.Success).expiresAt)
    }

    @Test
    fun `concurrent createChallenge requests with the same email return one success and one cooldown active`() {
        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val generated = CountDownLatch(2)
        val releaseGenerate = CountDownLatch(1)

        doAnswer {
            generated.countDown()
            assertTrue(releaseGenerate.await(5, TimeUnit.SECONDS))
            "123456"
        }.`when`(otpGenerator).generate(6)

        try {
            val futures =
                List(2) {
                    executor.submit<RequestChallengeResult> {
                        ready.countDown()
                        start.await(5, TimeUnit.SECONDS)
                        challengeService.createChallenge(
                            ChallengeCommand.Request(
                                email = "race@psycho.pizza",
                                operationType = OperationType.REGISTER,
                            ),
                        )
                    }
                }

            assertTrue(ready.await(5, TimeUnit.SECONDS))

            val results =
                assertTimeoutPreemptively(Duration.ofSeconds(10)) {
                    start.countDown()
                    assertTrue(generated.await(5, TimeUnit.SECONDS))
                    releaseGenerate.countDown()
                    futures.map { it.get(10, TimeUnit.SECONDS) }
                }

            assertEquals(1, results.count { it is RequestChallengeResult.Success })
            assertEquals(1, results.count { it is RequestChallengeResult.Failure.CooldownActive })
            val cooldownResult =
                results.first { it is RequestChallengeResult.Failure.CooldownActive } as
                    RequestChallengeResult.Failure.CooldownActive
            assertEquals(0, cooldownResult.retryAfterSeconds)
            assertEquals(1L, challengeRepository.count())
        } finally {
            executor.shutdownNow()
        }
    }
}
