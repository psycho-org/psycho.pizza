package pizza.psycho.sos.identity.security.principal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ActiveAccountPrincipalQueryService(
    private val accountRepository: AccountRepository,
) {
    fun findActivePrincipalByAccountId(accountId: UUID): AuthenticatedAccountPrincipal? =
        accountRepository.findActivePrincipalViewById(accountId)?.let {
            AuthenticatedAccountPrincipal(
                accountId = it.accountId,
                email = it.email,
            )
        }
}
