package pizza.psycho.sos.identity.challenge.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.identity.account.domain.vo.Email
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
    fun `findUsableByIdAndOperationTypeForUpdate returns token when it is unused and valid`() {
        val challenge =
            challengeRepository.save(
                Challenge.create(
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
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
                    targetEmail = Email.of("user@psycho.pizza"),
                    expiresAt = Instant.now().plusSeconds(300),
                ),
            )

        val found =
            confirmationTokenRepository.findUsableByIdAndOperationTypeForUpdate(
                savedToken.id(),
                OperationType.REGISTER,
                Instant.now(),
            )

        assertNotNull(found)
        assertEquals(savedToken.id(), found?.id())
    }

    @Test
    fun `findUsableByIdAndOperationTypeForUpdate returns null after token is consumed`() {
        val challenge =
            challengeRepository.save(
                Challenge.create(
                    operationType = OperationType.WITHDRAW,
                    targetEmail = Email.of("user@psycho.pizza"),
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
                    targetEmail = Email.of("user@psycho.pizza"),
                    expiresAt = Instant.now().plusSeconds(300),
                ),
            )
        token.consume()
        confirmationTokenRepository.save(token)

        val found =
            confirmationTokenRepository.findUsableByIdAndOperationTypeForUpdate(
                token.id(),
                OperationType.WITHDRAW,
                Instant.now(),
            )

        assertNull(found)
    }

    @Test
    fun `findUsableByIdAndOperationTypeForUpdate returns null when token is expired`() {
        val challenge =
            challengeRepository.save(
                Challenge.create(
                    operationType = OperationType.WITHDRAW,
                    targetEmail = Email.of("user@psycho.pizza"),
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
                    targetEmail = Email.of("user@psycho.pizza"),
                    expiresAt = Instant.now().minusSeconds(1),
                ),
            )

        val found =
            confirmationTokenRepository.findUsableByIdAndOperationTypeForUpdate(
                token.id(),
                OperationType.WITHDRAW,
                Instant.now(),
            )

        assertNull(found)
    }

    @Test
    fun `findUsableByIdAndOperationTypeForUpdate returns null when operation type differs`() {
        val challenge =
            challengeRepository.save(
                Challenge.create(
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
                    otpHash = "hash",
                    expiresAt = Instant.now().plusSeconds(300),
                    maxAttempts = 5,
                ),
            )
        val token =
            confirmationTokenRepository.save(
                ConfirmationToken.create(
                    challenge = challenge,
                    operationType = OperationType.REGISTER,
                    targetEmail = Email.of("user@psycho.pizza"),
                    expiresAt = Instant.now().plusSeconds(300),
                ),
            )

        val found =
            confirmationTokenRepository.findUsableByIdAndOperationTypeForUpdate(
                token.id(),
                OperationType.WITHDRAW,
                Instant.now(),
            )

        assertNull(found)
    }
}
