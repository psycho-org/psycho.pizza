package pizza.psycho.sos.identity.account.application

import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.common.support.transaction.runner.TransactionRunner
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.identity.account.application.service.WorkspaceOwnershipQueryService
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.identity.authentication.application.service.RefreshTokenService
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.domain.Challenge
import pizza.psycho.sos.identity.challenge.domain.ConfirmationToken
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.time.Instant
import java.util.UUID
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult as Register
import pizza.psycho.sos.identity.account.application.service.dto.UpdateNameAccountResult as UpdateName
import pizza.psycho.sos.identity.account.application.service.dto.UpdatePasswordAccountResult as UpdatePassword
import pizza.psycho.sos.identity.account.application.service.dto.WithdrawAccountResult as Withdraw

@ActiveProfiles("test")
class AccountServiceTests {
    private val accountRepository = mock(AccountRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val refreshTokenService = mock(RefreshTokenService::class.java)
    private val challengeService = mock(ChallengeService::class.java)
    private val workspaceOwnershipQueryService = mock(WorkspaceOwnershipQueryService::class.java)
    private val accountService =
        AccountService(
            accountRepository,
            passwordEncoder,
            refreshTokenService,
            challengeService,
            workspaceOwnershipQueryService,
        )

    private val testTokenId: UUID = UUID.fromString("00000000-0000-0000-0000-ffffffffffff")

    @BeforeEach
    fun setUp() {
        Tx.initialize(TransactionRunner())
    }

    @Test
    fun `find active display name by account id returns combined given and family name for active account`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000010")
        val account =
            Account
                .create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).also { it.id = accountId }

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)

        val displayName = accountService.findActiveDisplayNameByAccountIdOrNull(accountId)

        assertEquals("Rick Sanchez", displayName)
    }

    @Test
    fun `find active display name by account id returns null when account is not active`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000011")

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(null)

        val displayName = accountService.findActiveDisplayNameByAccountIdOrNull(accountId)

        assertNull(displayName)
    }

    @Test
    fun `register returns invalid name failure when given name is blank after trim`() {
        val command =
            AccountCommand.Register(
                confirmationTokenId = testTokenId,
                password = "Password123!",
                firstName = "   ",
                lastName = "Last",
            )

        val result = accountService.register(command)

        assertTrue(result is Register.Failure.InvalidName)
        verifyNoInteractions(challengeService)
    }

    @Test
    fun `register returns email already registered failure when email already exists`() {
        val command =
            AccountCommand.Register(
                confirmationTokenId = testTokenId,
                password = "Password123!",
                firstName = "First",
                lastName = "Last",
            )
        val token = usableToken(operationType = OperationType.REGISTER, email = "already@psycho.pizza")

        `when`(
            challengeService.acquireUsableToken(ChallengeCommand.AcquireToken(testTokenId, OperationType.REGISTER)),
        ).thenReturn(token)
        `when`(accountRepository.existsByEmailValueIgnoreCaseAndDeletedAtIsNull("already@psycho.pizza")).thenReturn(true)

        val result = accountService.register(command)
        assertTrue(result is Register.Failure.EmailAlreadyRegistered)
        assertTrue(!token.used)
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
        val token = usableToken(operationType = OperationType.REGISTER, email = "newuser@psycho.pizza")

        `when`(
            challengeService.acquireUsableToken(ChallengeCommand.AcquireToken(testTokenId, OperationType.REGISTER)),
        ).thenReturn(token)
        `when`(accountRepository.existsByEmailValueIgnoreCaseAndDeletedAtIsNull("newuser@psycho.pizza")).thenReturn(false)
        `when`(passwordEncoder.encode("Password123!")).thenReturn("encoded-password")
        `when`(accountRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Account::class.java))).thenAnswer { invocation ->
            val saved = invocation.getArgument<Account>(0)
            saved.id = UUID.fromString("00000000-0000-0000-0000-000000000111")
            saved
        }

        val result = accountService.register(command)
        val registered = result as Register.Success

        val captor = ArgumentCaptor.forClass(Account::class.java)
        verify(accountRepository).saveAndFlush(captor.capture())
        val saved = captor.value
        assertEquals("newuser@psycho.pizza", saved.email.value)
        assertEquals("encoded-password", saved.passwordHash)
        assertEquals("Rick", saved.givenName)
        assertEquals("Sanchez", saved.familyName)
        assertEquals("newuser@psycho.pizza", registered.email)
        assertEquals("Rick", registered.givenName)
        assertEquals("Sanchez", registered.familyName)
        assertTrue(token.used)
    }

    @Test
    fun `register maps account email unique constraint to email already registered`() {
        val command =
            AccountCommand.Register(
                confirmationTokenId = testTokenId,
                password = "Password123!",
                firstName = "Rick",
                lastName = "Sanchez",
            )
        val token = usableToken(operationType = OperationType.REGISTER, email = "race@psycho.pizza")

        `when`(
            challengeService.acquireUsableToken(ChallengeCommand.AcquireToken(testTokenId, OperationType.REGISTER)),
        ).thenReturn(token)
        `when`(accountRepository.existsByEmailValueIgnoreCaseAndDeletedAtIsNull("race@psycho.pizza")).thenReturn(false)
        `when`(passwordEncoder.encode("Password123!")).thenReturn("encoded-password")
        `when`(accountRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Account::class.java))).thenThrow(
            DataIntegrityViolationException(
                "duplicate account email",
                ConstraintViolationException(
                    "duplicate account email",
                    java.sql.SQLException("duplicate account email"),
                    "uk_accounts_email",
                ),
            ),
        )

        val result = accountService.register(command)

        assertEquals(Register.Failure.EmailAlreadyRegistered, result)
        assertTrue(!token.used)
        verifyNoInteractions(workspaceOwnershipQueryService)
    }

    @Test
    fun `update name trims and updates account`() {
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
            AccountCommand.Update.Name(
                accountId = accountId,
                givenName = "  Morty  ",
                familyName = "  Smith  ",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)

        val result = accountService.updateName(command)

        assertEquals(
            UpdateName.Success(
                givenName = "Morty",
                familyName = "Smith",
            ),
            result,
        )
        assertEquals("Morty", account.givenName)
        assertEquals("Smith", account.familyName)
    }

    @Test
    fun `update name returns invalid name failure for blank input`() {
        val command =
            AccountCommand.Update.Name(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000111"),
                givenName = "   ",
                familyName = "Smith",
            )

        val result = accountService.updateName(command)

        assertTrue(result is UpdateName.Failure.InvalidName)
    }

    @Test
    fun `update name returns invalid name failure when trimmed length exceeds 64`() {
        val command =
            AccountCommand.Update.Name(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000222"),
                givenName = "a".repeat(65),
                familyName = "Smith",
            )

        val result = accountService.updateName(command)

        assertTrue(result is UpdateName.Failure.InvalidName)
    }

    @Test
    fun `update name returns invalid name failure when input contains ISO control characters`() {
        val command =
            AccountCommand.Update.Name(
                accountId = UUID.fromString("00000000-0000-0000-0000-000000000333"),
                givenName = "Rick\u0000",
                familyName = "Smith",
            )

        val result = accountService.updateName(command)

        assertTrue(result is UpdateName.Failure.InvalidName)
    }

    @Test
    fun `update name returns account not found failure`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000222")
        val command =
            AccountCommand.Update.Name(
                accountId = accountId,
                givenName = "Summer",
                familyName = "Smith",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(null)

        val result = accountService.updateName(command)

        assertTrue(result is UpdateName.Failure.AccountNotFound)
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

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(null)

        val result = accountService.withdraw(command)

        assertTrue(result is Withdraw.Failure.AccountNotFound)
        verifyNoInteractions(challengeService)
        verifyNoInteractions(workspaceOwnershipQueryService)
        verify(
            passwordEncoder,
            never(),
        ).matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account::class.java))
        verify(refreshTokenService, never()).revokeAllByAccountId(accountId)
    }

    @Test
    fun `update password returns account not found failure before acquiring token`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000556")
        val command =
            AccountCommand.Update.Password(
                accountId = accountId,
                confirmationTokenId = testTokenId,
                currentPassword = "Password123!",
                newPassword = "NewPassword123!",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(null)

        val result = accountService.updatePassword(command)

        assertTrue(result is UpdatePassword.Failure.AccountNotFound)
        verifyNoInteractions(challengeService)
    }

    @Test
    fun `update password returns invalid credentials and leaves token unused`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000557")
        val account =
            Account
                .create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).also { it.id = accountId }
        val token = usableToken(operationType = OperationType.CHANGE_PASSWORD)
        val command =
            AccountCommand.Update.Password(
                accountId = accountId,
                confirmationTokenId = testTokenId,
                currentPassword = "WrongPassword!",
                newPassword = "NewPassword123!",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)
        `when`(
            challengeService.acquireUsableToken(ChallengeCommand.AcquireToken(testTokenId, OperationType.CHANGE_PASSWORD)),
        ).thenReturn(token)
        `when`(passwordEncoder.matches("WrongPassword!", "encoded-password")).thenReturn(false)

        val result = accountService.updatePassword(command)

        assertTrue(result is UpdatePassword.Failure.InvalidCredentials)
        assertTrue(!token.used)
        verify(passwordEncoder).matches("WrongPassword!", "encoded-password")
    }

    @Test
    fun `update password updates password hash and consumes token on success`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000558")
        val account =
            Account
                .create(
                    email = Email.of("user@psycho.pizza"),
                    passwordHash = "encoded-password",
                    givenName = "Rick",
                    familyName = "Sanchez",
                ).also { it.id = accountId }
        val token = usableToken(operationType = OperationType.CHANGE_PASSWORD)
        val command =
            AccountCommand.Update.Password(
                accountId = accountId,
                confirmationTokenId = testTokenId,
                currentPassword = "Password123!",
                newPassword = "NewPassword123!",
            )

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)
        `when`(
            challengeService.acquireUsableToken(ChallengeCommand.AcquireToken(testTokenId, OperationType.CHANGE_PASSWORD)),
        ).thenReturn(token)
        `when`(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true)
        `when`(passwordEncoder.encode("NewPassword123!")).thenReturn("new-password-hash")

        val result = accountService.updatePassword(command)

        assertTrue(result is UpdatePassword.Success)
        assertEquals("new-password-hash", account.passwordHash)
        assertTrue(token.used)
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
        val token = usableToken(operationType = OperationType.WITHDRAW)

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)
        `when`(
            challengeService.acquireUsableToken(ChallengeCommand.AcquireToken(testTokenId, OperationType.WITHDRAW)),
        ).thenReturn(token)
        `when`(passwordEncoder.matches("WrongPassword!", "encoded-password")).thenReturn(false)

        val result = accountService.withdraw(command)

        assertTrue(result is Withdraw.Failure.InvalidCredentials)
        assertTrue(!token.used)
        verify(passwordEncoder).matches("WrongPassword!", "encoded-password")
        verifyNoInteractions(workspaceOwnershipQueryService)
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account::class.java))
        verify(refreshTokenService, never()).revokeAllByAccountId(accountId)
    }

    @Test
    fun `withdraw returns owner workspace exists and leaves token unused`() {
        val accountId = UUID.fromString("00000000-0000-0000-0000-000000000776")
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
        val token = usableToken(operationType = OperationType.WITHDRAW)

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)
        `when`(
            challengeService.acquireUsableToken(ChallengeCommand.AcquireToken(testTokenId, OperationType.WITHDRAW)),
        ).thenReturn(token)
        `when`(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true)
        `when`(workspaceOwnershipQueryService.existsActiveOwnerMembershipByAccountId(accountId)).thenReturn(true)

        val result = accountService.withdraw(command)

        assertTrue(result is Withdraw.Failure.OwnerWorkspaceExists)
        assertTrue(!token.used)
        verify(workspaceOwnershipQueryService).existsActiveOwnerMembershipByAccountId(accountId)
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
        val token = usableToken(operationType = OperationType.WITHDRAW)

        `when`(accountRepository.findByIdAndDeletedAtIsNull(accountId)).thenReturn(account)
        `when`(
            challengeService.acquireUsableToken(ChallengeCommand.AcquireToken(testTokenId, OperationType.WITHDRAW)),
        ).thenReturn(token)
        `when`(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true)
        `when`(workspaceOwnershipQueryService.existsActiveOwnerMembershipByAccountId(accountId)).thenReturn(false)

        val result = accountService.withdraw(command)

        assertTrue(result is Withdraw.Success)
        assertTrue(account.isDeleted)
        assertEquals(accountId, account.deletedBy)
        assertTrue(token.used)
        verify(workspaceOwnershipQueryService).existsActiveOwnerMembershipByAccountId(accountId)
        verify(accountRepository).save(account)
        verify(refreshTokenService).revokeAllByAccountId(accountId)
    }

    private fun usableToken(
        operationType: OperationType,
        email: String = "user@psycho.pizza",
        tokenId: UUID = testTokenId,
    ): ConfirmationToken {
        val challenge =
            Challenge.create(
                operationType = operationType,
                targetEmail = Email.of(email),
                otpHash = "otp-hash",
                expiresAt = Instant.now().plusSeconds(300),
                maxAttempts = 3,
            )

        return ConfirmationToken
            .create(
                challenge = challenge,
                operationType = operationType,
                targetEmail = Email.of(email),
                expiresAt = Instant.now().plusSeconds(300),
            ).also { it.id = tokenId }
    }
}
