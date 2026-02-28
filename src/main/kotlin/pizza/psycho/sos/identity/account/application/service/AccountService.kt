package pizza.psycho.sos.identity.account.application.service

import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pizza.psycho.sos.identity.account.application.service.dto.AccountCommand
import pizza.psycho.sos.identity.account.application.service.dto.AccountResult
import pizza.psycho.sos.identity.account.application.service.dto.AccountSnapshot
import pizza.psycho.sos.identity.account.domain.Account
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun register(command: AccountCommand.Register): AccountResult {
        val email = command.email.trim().lowercase()
        if (accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            return AccountResult.Failure.EmailAlreadyRegistered
        }

        val account =
            Account.create(
                email = email,
                passwordHash = passwordEncoder.encode(command.password),
                givenName = command.firstName.trim(),
                familyName = command.lastName.trim(),
            )

        val saved = accountRepository.save(account)
        return AccountResult.Registered(
            account =
                AccountSnapshot(
                    id = saved.id.toString(),
                    email = saved.email.orEmpty(),
                    firstName = saved.givenName.orEmpty(),
                    lastName = saved.familyName.orEmpty(),
                ),
        )
    }
}
