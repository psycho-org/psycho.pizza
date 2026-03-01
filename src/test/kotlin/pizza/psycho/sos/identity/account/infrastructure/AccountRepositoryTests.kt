package pizza.psycho.sos.identity.account.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import pizza.psycho.sos.identity.account.domain.Account
import java.util.UUID

@DataJpaTest
@EnableJpaAuditing
class AccountRepositoryTests {
    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Test
    fun `existsByEmailIgnoreCaseAndDeletedAtIsNull returns true for active account with case-insensitive email`() {
        val account =
            accountRepository.save(
                Account.create(
                    email = "user@psycho.pizza",
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ),
            )
        assertNotNull(account.id)

        val exists = accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("USER@PSYCHO.PIZZA")
        assertTrue(exists)
    }

    @Test
    fun `findByEmailIgnoreCaseAndDeletedAtIsNull returns null for soft deleted account`() {
        val account =
            accountRepository.save(
                Account.create(
                    email = "user@psycho.pizza",
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ),
            )
        account.delete(UUID.fromString("00000000-0000-0000-0000-000000000999"))
        accountRepository.save(account)

        val found = accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("user@psycho.pizza")
        assertNull(found)
    }

    @Test
    fun `findByEmailIgnoreCaseAndDeletedAtIsNull returns active account`() {
        accountRepository.save(
            Account.create(
                email = "user@psycho.pizza",
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            ),
        )

        val found = accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("USER@psycho.pizza")

        val account = requireNotNull(found)
        assertEquals("user@psycho.pizza", account.email)
        assertFalse(account.isDeleted)
    }
}
