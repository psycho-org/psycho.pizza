package pizza.psycho.sos.identity.account.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.application.service.dto.AccountResult
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import java.util.UUID

@ActiveProfiles("test")
class AccountServiceTests {
    private val accountRepository = mock(AccountRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val accountService = AccountService(accountRepository, passwordEncoder)

    @Test
    fun `register returns email already registered failure when email already exists`() {
        val command =
            AccountCommand.Register(
                email = "already@psycho.pizza",
                password = "Password123!",
                firstName = "First",
                lastName = "Last",
            )

        `when`(accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("already@psycho.pizza")).thenReturn(true)

        val result = accountService.register(command)
        assertTrue(result is AccountResult.Failure.EmailAlreadyRegistered)
    }

    @Test
    fun `register saves normalized account and returns account payload`() {
        val command =
            AccountCommand.Register(
                email = "  NewUser@Psycho.Pizza ",
                password = "Password123!",
                firstName = " Rick ",
                lastName = " Sanchez ",
            )

        `when`(accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("newuser@psycho.pizza")).thenReturn(false)
        `when`(passwordEncoder.encode("Password123!")).thenReturn("encoded-password")
        `when`(accountRepository.save(org.mockito.ArgumentMatchers.any(Account::class.java))).thenAnswer { invocation ->
            val saved = invocation.getArgument<Account>(0)
            saved.id = UUID.fromString("00000000-0000-0000-0000-000000000111")
            saved
        }

        val result = accountService.register(command)
        val registered = result as AccountResult.Registered

        val captor = ArgumentCaptor.forClass(Account::class.java)
        verify(accountRepository).save(captor.capture())
        val saved = captor.value
        assertEquals("newuser@psycho.pizza", saved.email)
        assertEquals("encoded-password", saved.passwordHash)
        assertEquals("Rick", saved.givenName)
        assertEquals("Sanchez", saved.familyName)
        assertEquals("Rick Sanchez", saved.displayName)
        assertEquals("00000000-0000-0000-0000-000000000111", registered.account.id)
        assertEquals("newuser@psycho.pizza", registered.account.email)
        assertEquals("Rick", registered.account.firstName)
        assertEquals("Sanchez", registered.account.lastName)
    }
}
