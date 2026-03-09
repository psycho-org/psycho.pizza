package pizza.psycho.sos.identity.account.domain.vo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import pizza.psycho.sos.identity.account.domain.exception.InvalidEmailException

class EmailTests {
    @Test
    fun `of normalizes email by trimming and lowercasing`() {
        val email = Email.of("  User@Psycho.Pizza  ")

        assertEquals("user@psycho.pizza", email.value)
    }

    @Test
    fun `of throws when email is blank`() {
        assertThrows(InvalidEmailException::class.java) {
            Email.of("   ")
        }
    }

    @Test
    fun `of throws when email format is invalid`() {
        assertThrows(InvalidEmailException::class.java) {
            Email.of("invalid-email")
        }
    }
}
