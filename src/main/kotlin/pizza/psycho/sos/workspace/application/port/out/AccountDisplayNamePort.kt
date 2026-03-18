package pizza.psycho.sos.workspace.application.port.out

import java.util.UUID

interface AccountDisplayNamePort {
    fun findActiveDisplayNameByAccountId(accountId: UUID): String
}
