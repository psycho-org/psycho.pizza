package pizza.psycho.sos.identity.challenge.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Testcontainers
import pizza.psycho.sos.identity.account.domain.vo.Email
import pizza.psycho.sos.identity.challenge.application.port.VerificationDelivery
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.application.service.dto.RequestChallengeResult
import pizza.psycho.sos.identity.challenge.domain.Challenge
import pizza.psycho.sos.identity.challenge.domain.vo.ChallengeStatus
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import pizza.psycho.sos.identity.challenge.infrastructure.ChallengeRepository
import pizza.psycho.sos.identity.challenge.support.PostgresTestContainerSupport
import java.time.Instant

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

    @MockitoBean
    private lateinit var verificationDelivery: VerificationDelivery

    @BeforeEach
    fun cleanUp() {
        challengeRepository.deleteAll()
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
    }
}
