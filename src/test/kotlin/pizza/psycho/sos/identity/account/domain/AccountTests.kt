package pizza.psycho.sos.identity.account.domain

import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.domain.vo.Email
import kotlin.test.Test
import kotlin.test.assertEquals

@ActiveProfiles("test")
class AccountTests {
    @Test
    fun `create sets account fields`() {
        val account =
            Account.create(
                email = Email.of("user@psycho.pizza"),
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            )

        assertEquals("user@psycho.pizza", account.email.value)
        assertEquals("encoded-password", account.passwordHash)
        assertEquals("Rick", account.givenName)
        assertEquals("Sanchez", account.familyName)
    }

    @Test
    fun `create builds display name from given and family names`() {
        val account =
            Account.create(
                email = Email.of("user@psycho.pizza"),
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            )

        assertEquals("Rick Sanchez", account.displayName)
    }

    @Test
    fun `update display name changes display name`() {
        val account =
            Account.create(
                email = Email.of("user@psycho.pizza"),
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            )

        account.updateDisplayName("Pickle Rick")

        assertEquals("Pickle Rick", account.displayName)
    }

    @Test
    fun `update name changes given name and family name`() {
        val account =
            Account.create(
                email = Email.of("user@psycho.pizza"),
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            )

        account.updateName(givenName = "Morty", familyName = "Smith")

        assertEquals("Morty", account.givenName)
        assertEquals("Smith", account.familyName)
    }

    @Test
    fun `update password hash changes password hash`() {
        val account =
            Account.create(
                email = Email.of("user@psycho.pizza"),
                passwordHash = "old-hash",
                givenName = "Rick",
                familyName = "Sanchez",
            )

        account.updatePasswordHash("new-hash")

        assertEquals("new-hash", account.passwordHash)
    }
}
