package pizza.psycho.sos.identity.challenge.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.time.Instant

class ConfirmationTokenTests {
    @Test
    fun `create initializes token as unused with normalized email`() {
        val challenge =
            Challenge.create(
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("USER@Psycho.Pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )
        val token =
            ConfirmationToken.create(
                challenge = challenge,
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("USER@Psycho.Pizza"),
                expiresAt = Instant.now().plusSeconds(300),
            )

        assertFalse(token.used)
        assertEquals(Email.of("user@psycho.pizza"), token.targetEmail)
    }

    @Test
    fun `consume marks token as used and rejects second consume`() {
        val challenge =
            Challenge.create(
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("user@psycho.pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )
        val token =
            ConfirmationToken.create(
                challenge = challenge,
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("user@psycho.pizza"),
                expiresAt = Instant.now().plusSeconds(300),
            )

        token.consume()
        assertTrue(token.used)
        assertThrows(IllegalArgumentException::class.java) { token.consume() }
    }

    @Test
    fun `isExpired is true at expiry instant and after`() {
        val challenge =
            Challenge.create(
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("user@psycho.pizza"),
                otpHash = "hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )
        val expiresAt = Instant.now().plusSeconds(60)
        val token =
            ConfirmationToken.create(
                challenge = challenge,
                operationType = OperationType.REGISTER,
                targetEmail = Email.of("user@psycho.pizza"),
                expiresAt = expiresAt,
            )

        assertFalse(token.isExpired(expiresAt.minusSeconds(1)))
        assertTrue(token.isExpired(expiresAt))
        assertTrue(token.isExpired(expiresAt.plusSeconds(1)))
    }
}
