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

    @Test
    fun `update display name trims and updates account`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000111")
        val account =
            Account
                .create(
                    email = "user@psycho.pizza",
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).also { it.id = accountId }
        val command =
            AccountCommand.UpdateDisplayName(
                accountId = accountId,
                displayName = "  Pickle Rick  ",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)

        val result = accountService.updateDisplayName(command)

        assertEquals(
            AccountResult.Updated.DisplayName(
                displayName = "Pickle Rick",
            ),
            result,
        )
        assertEquals("Pickle Rick", account.displayName)
    }

    @Test
    fun `update display name returns invalid display name failure for blank input`() {
        val command =
            AccountCommand.UpdateDisplayName(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                displayName = "   ",
            )

        val result = accountService.updateDisplayName(command)

        assertTrue(result is AccountResult.Failure.InvalidDisplayName)
    }

    @Test
    fun `update display name returns account not found failure`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000222")
        val command =
            AccountCommand.UpdateDisplayName(
                accountId = accountId,
                displayName = "Summer",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(null)

        val result = accountService.updateDisplayName(command)

        assertTrue(result is AccountResult.Failure.AccountNotFound)
    }

    @Test
    fun `update display name accepts input longer than 40 only when trimmed value is within range`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000333")
        val account =
            Account
                .create(
                    email = "user@psycho.pizza",
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).also { it.id = accountId }
        val validTrimmedDisplayName = "a".repeat(40)
        val command =
            AccountCommand.UpdateDisplayName(
                accountId = accountId,
                displayName = "  $validTrimmedDisplayName  ",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)

        val result = accountService.updateDisplayName(command)

        assertEquals(
            AccountResult.Updated.DisplayName(
                displayName = validTrimmedDisplayName,
            ),
            result,
        )
    }

    @Test
    fun `update display name returns invalid display name failure when trimmed length exceeds 40`() {
        val command =
            AccountCommand.UpdateDisplayName(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000444"),
                displayName = "a".repeat(41),
            )

        val result = accountService.updateDisplayName(command)

        assertTrue(result is AccountResult.Failure.InvalidDisplayName)
    }
}
