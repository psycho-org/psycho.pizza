package pizza.psycho.sos.identity.account.application.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.common.domain.vo.Email
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.common.support.transaction.helper.hasConstraintName
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.identity.authentication.application.service.RefreshTokenService
import pizza.psycho.sos.identity.challenge.application.service.ChallengeService
import pizza.psycho.sos.identity.challenge.application.service.dto.ChallengeCommand
import pizza.psycho.sos.identity.challenge.domain.vo.OperationType
import java.util.UUID
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult as Register
import pizza.psycho.sos.identity.account.application.service.dto.UpdateNameAccountResult as UpdateName
import pizza.psycho.sos.identity.account.application.service.dto.UpdatePasswordAccountResult as UpdatePassword
import pizza.psycho.sos.identity.account.application.service.dto.WithdrawAccountResult as Withdraw

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenService: RefreshTokenService,
    private val challengeService: ChallengeService,
    private val workspaceOwnershipQueryService: WorkspaceOwnershipQueryService,
) {
    fun findActiveAccountIdByEmailOrNull(email: String): UUID? =
        accountRepository
            .findByEmailValueIgnoreCaseAndDeletedAtIsNull(Email.of(email).value)
            ?.id

    fun findActiveDisplayNameByAccountIdOrNull(accountId: UUID): String? =
        accountRepository
            .findByIdAndDeletedAtIsNull(accountId)
            ?.let { "${it.givenName} ${it.familyName}" }

    fun register(command: AccountCommand.Register): Register =
        try {
            Tx.writable { registerInTransaction(command) }
        } catch (ex: RuntimeException) {
            if (ex.hasConstraintName(ACCOUNT_EMAIL_CONSTRAINT_NAME)) {
                Register.Failure.EmailAlreadyRegistered
            } else {
                throw ex
            }
        }

    @Transactional
    fun updateName(command: AccountCommand.Update.Name): UpdateName {
        val normalizedGivenName = normalizeName(command.givenName) ?: return UpdateName.Failure.InvalidName
        val normalizedFamilyName = normalizeName(command.familyName) ?: return UpdateName.Failure.InvalidName

        val account =
            accountRepository.findByIdAndDeletedAtIsNull(command.accountId)
                ?: return UpdateName.Failure.AccountNotFound

        account.updateName(givenName = normalizedGivenName, familyName = normalizedFamilyName)
        return UpdateName.Success(
            givenName = normalizedGivenName,
            familyName = normalizedFamilyName,
        )
    }

    @Transactional
    fun updatePassword(command: AccountCommand.Update.Password): UpdatePassword {
        val account =
            accountRepository.findByIdAndDeletedAtIsNull(command.accountId)
                ?: return UpdatePassword.Failure.AccountNotFound

        val token =
            challengeService.acquireUsableToken(
                ChallengeCommand.AcquireToken(command.confirmationTokenId, OperationType.CHANGE_PASSWORD),
            ) ?: return UpdatePassword.Failure.InvalidConfirmationToken

        if (account.email != token.targetEmail) {
            return UpdatePassword.Failure.InvalidConfirmationToken
        }

        if (!passwordEncoder.matches(command.currentPassword, account.passwordHash)) {
            return UpdatePassword.Failure.InvalidCredentials
        }

        account.updatePasswordHash(passwordEncoder.encode(command.newPassword))
        token.consume()
        return UpdatePassword.Success
    }

    @Transactional
    fun withdraw(command: AccountCommand.Withdraw): Withdraw {
        val account =
            accountRepository.findByIdAndDeletedAtIsNull(command.accountId)
                ?: return Withdraw.Failure.AccountNotFound

        val token =
            challengeService.acquireUsableToken(
                ChallengeCommand.AcquireToken(command.confirmationTokenId, OperationType.WITHDRAW),
            ) ?: return Withdraw.Failure.InvalidConfirmationToken

        if (account.email != token.targetEmail) {
            return Withdraw.Failure.InvalidConfirmationToken
        }

        if (!passwordEncoder.matches(command.password, account.passwordHash)) {
            return Withdraw.Failure.InvalidCredentials
        }

        if (workspaceOwnershipQueryService.existsActiveOwnerMembershipByAccountId(command.accountId)) {
            return Withdraw.Failure.OwnerWorkspaceExists
        }

        account.delete(command.accountId)
        accountRepository.save(account)
        refreshTokenService.revokeAllByAccountId(command.accountId)
        token.consume()
        return Withdraw.Success
    }

    private fun registerInTransaction(command: AccountCommand.Register): Register {
        val normalizedGivenName = normalizeName(command.firstName) ?: return Register.Failure.InvalidName
        val normalizedFamilyName = normalizeName(command.lastName) ?: return Register.Failure.InvalidName

        val token =
            challengeService.acquireUsableToken(
                ChallengeCommand.AcquireToken(command.confirmationTokenId, OperationType.REGISTER),
            )
                ?: return Register.Failure.InvalidConfirmationToken

        val email = token.targetEmail
        if (accountRepository.existsByEmailValueIgnoreCaseAndDeletedAtIsNull(email.value)) {
            return Register.Failure.EmailAlreadyRegistered
        }

        val account =
            Account.create(
                email = email,
                passwordHash = passwordEncoder.encode(command.password),
                givenName = normalizedGivenName,
                familyName = normalizedFamilyName,
            )

        val saved = accountRepository.saveAndFlush(account)
        token.consume()

        return Register.Success(
            email = saved.email.value,
            givenName = saved.givenName,
            familyName = saved.familyName,
        )
    }

    private fun normalizeName(value: String): String? {
        val normalized = value.trim()
        if (normalized.isBlank()) {
            return null
        }
        if (normalized.length > NAME_MAX_LENGTH) {
            return null
        }
        if (normalized.any { Character.isISOControl(it) }) {
            return null
        }
        return normalized
    }

    companion object {
        private const val ACCOUNT_EMAIL_CONSTRAINT_NAME = "uk_accounts_email"
        private const val NAME_MAX_LENGTH = 64
    }
}
