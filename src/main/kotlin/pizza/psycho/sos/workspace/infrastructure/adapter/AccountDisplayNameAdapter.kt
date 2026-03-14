package pizza.psycho.sos.workspace.infrastructure.adapter

import org.springframework.stereotype.Component
import pizza.psycho.sos.identity.account.infrastructure.AccountRepository
import pizza.psycho.sos.workspace.application.port.out.AccountDisplayNamePort
import java.util.UUID

@Component
class AccountDisplayNameAdapter(
    private val accountRepository: AccountRepository,
) : AccountDisplayNamePort {
    override fun findActiveDisplayNameByAccountIdOrNull(accountId: UUID): String? =
        accountRepository.findByIdAndDeletedAtIsNull(accountId)?.let { "${it.givenName} ${it.familyName}".trim() }
}
