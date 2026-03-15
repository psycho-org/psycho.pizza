package pizza.psycho.sos.workspace.infrastructure.adapter

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.identity.account.application.service.WorkspaceMembershipCleanupPort
import pizza.psycho.sos.workspace.domain.repository.WorkspaceMembershipCommandRepository
import java.util.UUID

@Component
class WorkspaceMembershipCleanupAdapter(
    private val workspaceMembershipCommandRepository: WorkspaceMembershipCommandRepository,
) : WorkspaceMembershipCleanupPort {
    @Transactional
    override fun softDeleteActiveMembershipsByAccountId(
        accountId: UUID,
        deletedBy: UUID,
    ) {
        workspaceMembershipCommandRepository.findActiveNonOwnerMembershipsByAccountId(accountId).forEach { membership ->
            membership.delete(deletedBy)
        }
    }
}
