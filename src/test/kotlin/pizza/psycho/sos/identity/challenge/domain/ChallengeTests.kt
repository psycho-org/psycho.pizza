package pizza.psycho.sos.identity.challenge.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.identity.challenge.domain.vo.ChallengeStatus
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.time.Instant

class ChallengeTests {
    @Test
    fun `create initializes pending challenge with normalized target email`() {
        val challenge =
            Challenge.create(
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("USER@Psycho.Pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )

        assertEquals(ChallengeStatus.PENDING, challenge.status)
        assertEquals(Email.of("user@psycho.pizza"), challenge.targetEmail)
        assertEquals(0, challenge.attemptCount)
        assertEquals(3, challenge.maxAttempts)
    }

    @Test
    fun `incrementAttempt and hasExceededMaxAttempts follow configured max`() {
        val challenge =
            Challenge.create(
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("user@psycho.pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 2,
            )

        assertFalse(challenge.hasExceededMaxAttempts())
        challenge.incrementAttempt()
        assertFalse(challenge.hasExceededMaxAttempts())
        challenge.incrementAttempt()
        assertTrue(challenge.hasExceededMaxAttempts())
    }

    @Test
    fun `isExpired is true at expiry instant and after`() {
        val expiresAt = Instant.now().plusSeconds(60)
        val challenge =
            Challenge.create(
                operationType = OperationType.WITHDRAW,
                targetEmail = Email.of("user@psycho.pizza"),
                otpHash = "hash",
                expiresAt = expiresAt,
                maxAttempts = 3,
            )

        assertFalse(challenge.isExpired(expiresAt.minusSeconds(1)))
        assertTrue(challenge.isExpired(expiresAt))
        assertTrue(challenge.isExpired(expiresAt.plusSeconds(1)))
    }

    @Test
    fun `mark methods transition challenge out of pending`() {
        val verified =
            Challenge.create(
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("user@psycho.pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )
        verified.markVerified()
        assertEquals(ChallengeStatus.VERIFIED, verified.status)

        val failed =
            Challenge.create(
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("user@psycho.pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )
        failed.markFailed()
        assertEquals(ChallengeStatus.FAILED, failed.status)

        val expired =
            Challenge.create(
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("user@psycho.pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )
        expired.markExpired()
        assertEquals(ChallengeStatus.EXPIRED, expired.status)
    }
}
