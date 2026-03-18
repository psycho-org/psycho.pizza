package pizza.psycho.sos.workspace.infrastructure.adapter

import org.springframework.stereotype.Component
import pizza.psycho.sos.identity.account.application.service.AccountService
import pizza.psycho.sos.workspace.application.port.out.AccountDisplayNamePort
import java.util.UUID

@Component
class AccountDisplayNameAdapter(
    private val accountService: AccountService,
) : AccountDisplayNamePort {
    override fun findActiveDisplayNameByAccountId(accountId: UUID): String = accountService.findActiveDisplayNameByAccountId(accountId)
}
