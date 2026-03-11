package pizza.psycho.sos.identity.challenge.infrastructure

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.identity.challenge.domain.Challenge
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import pizza.psycho.sos.identity.challenge.support.PostgresTestContainerSupport
import java.time.Instant

@Tag("tc")
@Testcontainers
@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test-tc")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChallengeRepositoryPostgresTests : PostgresTestContainerSupport() {
    @Autowired
    private lateinit var challengeRepository: ChallengeRepository

    @BeforeEach
    fun cleanUp() {
        challengeRepository.deleteAllInBatch()
    }

    @AfterEach
    fun tearDown() {
        challengeRepository.deleteAllInBatch()
    }

    @Test
    fun `duplicate pending challenges for same email and operation are rejected`() {
        challengeRepository.saveAndFlush(newChallenge(email = "user@psycho.pizza", operationType = OperationType.REGISTER))

        assertThrows<DataIntegrityViolationException> {
            challengeRepository.saveAndFlush(newChallenge(email = "user@psycho.pizza", operationType = OperationType.REGISTER))
        }
    }

    @Test
    fun `new pending challenge is allowed after previous pending is expired`() {
        val existing =
            challengeRepository.saveAndFlush(
                newChallenge(email = "user@psycho.pizza", operationType = OperationType.REGISTER),
            )
        existing.markExpired()
        challengeRepository.saveAndFlush(existing)

        val saved = challengeRepository.saveAndFlush(newChallenge(email = "user@psycho.pizza", operationType = OperationType.REGISTER))

        assertNotNull(saved.id)
    }

    @Test
    fun `pending uniqueness is case-insensitive for target email`() {
        challengeRepository.saveAndFlush(newChallenge(email = "user@psycho.pizza", operationType = OperationType.WITHDRAW))

        assertThrows<DataIntegrityViolationException> {
            challengeRepository.saveAndFlush(newChallenge(email = "USER@PSYCHO.PIZZA", operationType = OperationType.WITHDRAW))
        }
    }

    private fun newChallenge(
        email: String,
        operationType: OperationType,
    ): Challenge =
        Challenge.create(
            operationType = operationType,
            targetEmail = Email.of(email),
            otpHash = "otp-hash",
            expiresAt = Instant.now().plusSeconds(300),
            maxAttempts = 5,
        )
}
