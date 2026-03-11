package pizza.psycho.sos.common.domain.vo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import pizza.psycho.sos.common.exception.CommonErrorCode
import pizza.psycho.sos.common.handler.DomainException

class EmailTests {
    @Test
    fun `of normalizes email by trimming and lowercasing`() {
        val email = Email.of("  User@Psycho.Pizza  ")

        assertEquals("user@psycho.pizza", email.value)
    }

    @Test
    fun `of throws when email is blank`() {
        val exception =
            assertThrows(DomainException::class.java) {
                Email.of("   ")
            }

        assertEquals(CommonErrorCode.COMMON_INVALID_EMAIL, exception.errorCode)
    }

    @Test
    fun `of throws when email format is invalid`() {
        val exception =
            assertThrows(DomainException::class.java) {
                Email.of("invalid-email")
            }

        assertEquals(CommonErrorCode.COMMON_INVALID_EMAIL, exception.errorCode)
    }
}
