package pizza.psycho.sos.identity.account.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.domain.vo.Email
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.identity.authentication.application.service.RefreshTokenService
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.application.service.dto.ConsumeTokenResult
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.util.UUID
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult as Register
import pizza.psycho.sos.identity.account.application.service.dto.UpdateDisplayNameAccountResult as UpdateDisplayName
import pizza.psycho.sos.identity.account.application.service.dto.WithdrawAccountResult as Withdraw

@ActiveProfiles("test")
class AccountServiceTests {
    private val accountRepository = mock(AccountRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val refreshTokenService = mock(RefreshTokenService::class.java)
    private val challengeService = mock(ChallengeService::class.java)
    private val accountService = AccountService(accountRepository, passwordEncoder, refreshTokenService, challengeService)

    private val testTokenId: UUID = UUID.fromString("00000000-0000-0000-0000-ffffffffffff")

    @Test
    fun `register returns email already registered failure when email already exists`() {
        val command =
            AccountCommand.Register(
                confirmationTokenId = testTokenId,
                password = "Password123!",
                firstName = "First",
                lastName = "Last",
            )

        `when`(
            challengeService.consumeToken(ChallengeCommand.ConsumeToken(testTokenId, OperationType.REGISTER)),
        ).thenReturn(ConsumeTokenResult.Success(targetEmail = "already@psycho.pizza"))
        `when`(accountRepository.existsByEmailValueIgnoreCaseAndDeletedAtIsNull("already@psycho.pizza")).thenReturn(true)

        val result = accountService.register(command)
        assertTrue(result is Register.Failure.EmailAlreadyRegistered)
    }

    @Test
    fun `register saves normalized account and returns account payload`() {
        val command =
            AccountCommand.Register(
                confirmationTokenId = testTokenId,
                password = "Password123!",
                firstName = " Rick ",
                lastName = " Sanchez ",
            )

        `when`(
            challengeService.consumeToken(ChallengeCommand.ConsumeToken(testTokenId, OperationType.REGISTER)),
        ).thenReturn(ConsumeTokenResult.Success(targetEmail = "newuser@psycho.pizza"))
        `when`(accountRepository.existsByEmailValueIgnoreCaseAndDeletedAtIsNull("newuser@psycho.pizza")).thenReturn(false)
        `when`(passwordEncoder.encode("Password123!")).thenReturn("encoded-password")
        `when`(accountRepository.save(org.mockito.ArgumentMatchers.any(Account::class.java))).thenAnswer { invocation ->
            val saved = invocation.getArgument<Account>(0)
            saved.id = UUID.fromString("00000000-0000-0000-0000-000000000111")
            saved
        }

        val result = accountService.register(command)
        val registered = result as Register.Success

        val captor = ArgumentCaptor.forClass(Account::class.java)
        verify(accountRepository).save(captor.capture())
        val saved = captor.value
        assertEquals("newuser@psycho.pizza", saved.email.value)
        assertEquals("encoded-password", saved.passwordHash)
        assertEquals("Rick", saved.givenName)
        assertEquals("Sanchez", saved.familyName)
        assertEquals("Rick Sanchez", saved.displayName)
        assertEquals("newuser@psycho.pizza", registered.email)
        assertEquals("Rick Sanchez", registered.displayName)
    }

    @Test
    fun `update display name trims and updates account`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000111")
        val account =
            Account
                .create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).also { it.id = accountId }
        val command =
            AccountCommand.Update.DisplayName(
                accountId = accountId,
                displayName = "  Pickle Rick  ",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)

        val result = accountService.updateDisplayName(command)

        assertEquals(
            UpdateDisplayName.Success(
                displayName = "Pickle Rick",
            ),
            result,
        )
        assertEquals("Pickle Rick", account.displayName)
    }

    @Test
    fun `update display name returns invalid display name failure for blank input`() {
        val command =
            AccountCommand.Update.DisplayName(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                displayName = "   ",
            )

        val result = accountService.updateDisplayName(command)

        assertTrue(result is UpdateDisplayName.Failure.InvalidDisplayName)
    }

    @Test
    fun `update display name returns account not found failure`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000222")
        val command =
            AccountCommand.Update.DisplayName(
                accountId = accountId,
                displayName = "Summer",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(null)

        val result = accountService.updateDisplayName(command)

        assertTrue(result is UpdateDisplayName.Failure.AccountNotFound)
    }

    @Test
    fun `update display name accepts input longer than 40 only when trimmed value is within range`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000333")
        val account =
            Account
                .create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).also { it.id = accountId }
        val validTrimmedDisplayName = "a".repeat(40)
        val command =
            AccountCommand.Update.DisplayName(
                accountId = accountId,
                displayName = "  $validTrimmedDisplayName  ",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)

        val result = accountService.updateDisplayName(command)

        assertEquals(
            UpdateDisplayName.Success(
                displayName = validTrimmedDisplayName,
            ),
            result,
        )
    }

    @Test
    fun `update display name returns invalid display name failure when trimmed length exceeds 64`() {
        val command =
            AccountCommand.Update.DisplayName(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000444"),
                displayName = "a".repeat(65),
            )

        val result = accountService.updateDisplayName(command)

        assertTrue(result is UpdateDisplayName.Failure.InvalidDisplayName)
    }

    @Test
    fun `withdraw returns account not found failure when account does not exist`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000555")
        val command =
            AccountCommand.Withdraw(
                accountId = accountId,
                confirmationTokenId = testTokenId,
                password = "Password123!",
            )

        `when`(
            challengeService.consumeToken(ChallengeCommand.ConsumeToken(testTokenId, OperationType.WITHDRAW)),
        ).thenReturn(ConsumeTokenResult.Success(targetEmail = "user@psycho.pizza"))
        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(null)

        val result = accountService.withdraw(command)

        assertTrue(result is Withdraw.Failure.AccountNotFound)
        verify(
            passwordEncoder,
            never(),
        ).matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account::class.java))
        verify(refreshTokenService, never()).revokeAllByAccountId(accountId)
    }

    @Test
    fun `withdraw returns invalid credentials when password does not match`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000666")
        val account =
            Account
                .create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).also { it.id = accountId }
        val command =
            AccountCommand.Withdraw(
                accountId = accountId,
                confirmationTokenId = testTokenId,
                password = "WrongPassword!",
            )

        `when`(
            challengeService.consumeToken(ChallengeCommand.ConsumeToken(testTokenId, OperationType.WITHDRAW)),
        ).thenReturn(ConsumeTokenResult.Success(targetEmail = "user@psycho.pizza"))
        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)
        `when`(passwordEncoder.matches("WrongPassword!", "encoded-password")).thenReturn(false)

        val result = accountService.withdraw(command)

        assertTrue(result is Withdraw.Failure.InvalidCredentials)
        verify(passwordEncoder).matches("WrongPassword!", "encoded-password")
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account::class.java))
        verify(refreshTokenService, never()).revokeAllByAccountId(accountId)
    }

    @Test
    fun `withdraw soft deletes account and revokes refresh tokens when password matches`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000777")
        val account =
            Account
                .create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).also { it.id = accountId }
        val command =
            AccountCommand.Withdraw(
                accountId = accountId,
                confirmationTokenId = testTokenId,
                password = "Password123!",
            )

        `when`(
            challengeService.consumeToken(ChallengeCommand.ConsumeToken(testTokenId, OperationType.WITHDRAW)),
        ).thenReturn(ConsumeTokenResult.Success(targetEmail = "user@psycho.pizza"))
        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)
        `when`(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true)

        val result = accountService.withdraw(command)

        assertTrue(result is Withdraw.Success)
        assertTrue(account.isDeleted)
        assertEquals(accountId, account.deletedBy)
        verify(accountRepository).save(account)
        verify(refreshTokenService).revokeAllByAccountId(accountId)
    }
}
