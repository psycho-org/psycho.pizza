package pizza.psycho.sos.identity.account.application.service

import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.domain.vo.Email
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.identity.authentication.application.service.RefreshTokenService
import pizza.psycho.sos.identity.account.application.service.dto.RegisterAccountResult as Register
import pizza.psycho.sos.identity.account.application.service.dto.UpdateAccountResult as Update
import pizza.psycho.sos.identity.account.application.service.dto.WithdrawAccountResult as Withdraw

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenService: RefreshTokenService,
) {
    fun register(command: AccountCommand.Register): Register {
        val email = Email.of(command.email)
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
            displayName = saved.displayName!!,
        )
    }

    fun updateDisplayName(command: AccountCommand.Update.DisplayName): Update {
        val normalizedDisplayName = command.displayName.trim()
        if (normalizedDisplayName.length !in DISPLAY_NAME_LENGTH_RANGE) {
            return Update.Failure.InvalidDisplayName
        }

        val account =
            accountRepository.findByIdAndDeletedAtIsNull(command.accountId)
                ?: return Update.Failure.AccountNotFound

        account.updateDisplayName(normalizedDisplayName)
        return Update.Success.DisplayName(
            displayName = normalizedDisplayName,
        )
    }

    fun withdraw(command: AccountCommand.Withdraw): Withdraw {
        val account =
            accountRepository.findByIdAndDeletedAtIsNull(command.accountId)
                ?: return Withdraw.Failure.AccountNotFound

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
        private val DISPLAY_NAME_LENGTH_RANGE = 1..40
    }
}
