package pizza.psycho.sos.identity.account.application.service

import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
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
import pizza.psycho.sos.identity.account.application.service.dto.UpdateNameAccountResult as UpdateName
import pizza.psycho.sos.identity.account.application.service.dto.UpdatePasswordAccountResult as UpdatePassword
import pizza.psycho.sos.identity.account.application.service.dto.WithdrawAccountResult as Withdraw

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenService: RefreshTokenService,
    private val challengeService: ChallengeService,
) {
    fun findActiveAccountIdByEmailOrNull(email: String): UUID? =
        accountRepository
            .findByEmailValueIgnoreCaseAndDeletedAtIsNull(Email.of(email).value)
            ?.id

    fun register(command: AccountCommand.Register): Register {
        val tokenResult =
            challengeService.consumeToken(
                ChallengeCommand.ConsumeToken(command.confirmationTokenId, OperationType.REGISTER),
            ) as? ConsumeTokenResult.Success
                ?: return Register.Failure.InvalidConfirmationToken

        val email = tokenResult.targetEmail
        if (accountRepository.existsByEmailValueIgnoreCaseAndDeletedAtIsNull(email.value)) {
            return Register.Failure.EmailAlreadyRegistered
        }

        val account =
            Account.create(
                email = email,
                passwordHash = passwordEncoder.encode(command.password),
                givenName = command.firstName.trim(),
                familyName = command.lastName.trim(),
            )

        val saved = accountRepository.save(account)

        return Register.Success(
            email = saved.email.value,
            displayName = saved.displayName,
        )
    }

    fun updateDisplayName(command: AccountCommand.Update.DisplayName): UpdateDisplayName {
        val normalizedDisplayName = command.displayName.trim()
        if (normalizedDisplayName.length !in DISPLAY_NAME_LENGTH_RANGE) {
            return UpdateDisplayName.Failure.InvalidDisplayName
        }

        val account =
            accountRepository.findByIdAndDeletedAtIsNull(command.accountId)
                ?: return UpdateDisplayName.Failure.AccountNotFound

        account.updateDisplayName(normalizedDisplayName)
        return UpdateDisplayName.Success(
            displayName = normalizedDisplayName,
        )
    }

    fun updateName(command: AccountCommand.Update.Name): UpdateName {
        val normalizedGivenName = command.givenName.trim()
        val normalizedFamilyName = command.familyName.trim()

        // TODO - add validation layer

        val account =
            accountRepository.findByIdAndDeletedAtIsNull(command.accountId)
                ?: return UpdateName.Failure.AccountNotFound

        account.updateName(givenName = normalizedGivenName, familyName = normalizedFamilyName)
        return UpdateName.Success(
            givenName = normalizedGivenName,
            familyName = normalizedFamilyName,
        )
    }

    fun updatePassword(command: AccountCommand.Update.Password): UpdatePassword {
        val tokenResult =
            challengeService.consumeToken(
                ChallengeCommand.ConsumeToken(command.confirmationTokenId, OperationType.CHANGE_PASSWORD),
            ) as? ConsumeTokenResult.Success
                ?: return UpdatePassword.Failure.InvalidConfirmationToken

        val account =
            accountRepository.findByIdAndDeletedAtIsNull(command.accountId)
                ?: return UpdatePassword.Failure.AccountNotFound

        if (account.email != tokenResult.targetEmail) {
            return UpdatePassword.Failure.InvalidConfirmationToken
        }

        if (!passwordEncoder.matches(command.currentPassword, account.passwordHash)) {
            return UpdatePassword.Failure.InvalidCredentials
        }

        account.updatePasswordHash(passwordEncoder.encode(command.newPassword))
        return UpdatePassword.Success
    }

    fun withdraw(command: AccountCommand.Withdraw): Withdraw {
        val tokenResult =
            challengeService.consumeToken(
                ChallengeCommand.ConsumeToken(command.confirmationTokenId, OperationType.WITHDRAW),
            ) as? ConsumeTokenResult.Success
                ?: return Withdraw.Failure.InvalidConfirmationToken

        val account =
            accountRepository.findByIdAndDeletedAtIsNull(command.accountId)
                ?: return Withdraw.Failure.AccountNotFound

        if (account.email != tokenResult.targetEmail) {
            return Withdraw.Failure.InvalidConfirmationToken
        }

        if (!passwordEncoder.matches(command.password, account.passwordHash)) {
            return Withdraw.Failure.InvalidCredentials
        }

//        TODO - membership service required
//        if (membershipService.existsActiveOwnerMembershipByAccountId(command.accountId)) {
//           return Withdraw.Failure.OwnerWorkspaceExists
//        }

        account.delete(command.accountId)
        accountRepository.save(account)
        refreshTokenService.revokeAllByAccountId(command.accountId)
        return Withdraw.Success
    }

    companion object {
        private val DISPLAY_NAME_LENGTH_RANGE = 1..64
    }
}
