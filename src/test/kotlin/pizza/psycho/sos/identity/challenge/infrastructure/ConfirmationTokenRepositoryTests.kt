package pizza.psycho.sos.identity.challenge.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.identity.challenge.domain.Challenge
import pizza.psycho.sos.identity.challenge.domain.ConfirmationToken
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.time.Instant

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class ConfirmationTokenRepositoryTests {
    @Autowired
    private lateinit var challengeRepository: ChallengeRepository

    @Autowired
    private lateinit var confirmationTokenRepository: ConfirmationTokenRepository

    @Test
    fun `findByIdAndUsedFalse returns token when it is unused`() {
        val challenge =
            challengeRepository.save(
                Challenge.create(
                    operationType = OperationType.REGISTER,
                    targetEmail = "user@psycho.pizza",
                    otpHash = "hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 5,
                ),
            )
        val savedToken =
            confirmationTokenRepository.save(
                ConfirmationToken.create(
                    challenge = challenge,
                    operationType = OperationType.REGISTER,
                    targetEmail = "user@psycho.pizza",
                    expiresAt = Instant.now().plusSeconds(300),
                ),
            )

        val found = confirmationTokenRepository.findByIdAndUsedFalse(savedToken.id())

        assertNotNull(found)
        assertEquals(savedToken.id(), found?.id())
    }

    @Test
    fun `findByIdAndUsedFalse returns null after token is consumed`() {
        val challenge =
            challengeRepository.save(
                Challenge.create(
                    operationType = OperationType.WITHDRAW,
                    targetEmail = "user@psycho.pizza",
                    otpHash = "hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 5,
                ),
            )
        val token =
            confirmationTokenRepository.save(
                ConfirmationToken.create(
                    challenge = challenge,
                    operationType = OperationType.WITHDRAW,
                    targetEmail = "user@psycho.pizza",
                    expiresAt = Instant.now().plusSeconds(300),
                ),
            )
        token.consume()
        confirmationTokenRepository.save(token)

        val found = confirmationTokenRepository.findByIdAndUsedFalse(token.id())

        assertNull(found)
    }
}
