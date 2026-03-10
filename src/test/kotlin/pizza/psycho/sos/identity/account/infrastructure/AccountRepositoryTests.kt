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
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.domain.vo.Email
import java.util.UUID

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class AccountRepositoryTests {
    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Test
    fun `existsByEmailValueIgnoreCaseAndDeletedAtIsNull returns true for active account with case-insensitive email`() {
        val account =
            accountRepository.save(
                Account.create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ),
            )
        assertNotNull(account.id)

        val exists = accountRepository.existsByEmailValueIgnoreCaseAndDeletedAtIsNull("USER@PSYCHO.PIZZA")
        assertTrue(exists)
    }

    @Test
    fun `findByEmailValueIgnoreCaseAndDeletedAtIsNull returns null for soft deleted account`() {
        val account =
            accountRepository.save(
                Account.create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ),
            )
        account.delete(UUID.fromString("00000000-0000-0000-0000-000000000999"))
        accountRepository.save(account)

        val found = accountRepository.findByEmailValueIgnoreCaseAndDeletedAtIsNull("user@psycho.pizza")
        assertNull(found)
    }

    @Test
    fun `findByEmailValueIgnoreCaseAndDeletedAtIsNull returns active account`() {
        accountRepository.save(
            Account.create(
                email = Email.of("user@psycho.pizza"),
                passwordHash = "encoded-password",
                givenName = "Rick",
                familyName = "Sanchez",
            ),
        )

        val found = accountRepository.findByEmailValueIgnoreCaseAndDeletedAtIsNull("USER@psycho.pizza")

        val account = requireNotNull(found)
        assertEquals("user@psycho.pizza", account.email.value)
        assertFalse(account.isDeleted)
    }
}
