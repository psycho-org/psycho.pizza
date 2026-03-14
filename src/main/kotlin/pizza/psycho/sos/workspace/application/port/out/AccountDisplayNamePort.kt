package pizza.psycho.sos.workspace.application.port.out

import java.util.UUID

// TODO account adapter 요청 필요
interface AccountDisplayNamePort {
    fun findActiveDisplayNameByAccountIdOrNull(accountId: UUID): String?
}
