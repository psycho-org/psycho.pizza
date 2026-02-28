package pizza.psycho.sos.identity.account.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class AccountTests {
    @Test
    fun `create sets account fields`() {
        val account =
            Account.create(
                email = "user@psycho.pizza",
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            )

        assertEquals("user@psycho.pizza", account.email)
        assertEquals("encoded-password", account.passwordHash)
        assertEquals("Rick", account.givenName)
        assertEquals("Sanchez", account.familyName)
    }

    @Test
    fun `create builds display name from given and family names`() {
        val account =
            Account.create(
                email = "user@psycho.pizza",
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            )

        assertEquals("Rick Sanchez", account.displayName)
    }
}
