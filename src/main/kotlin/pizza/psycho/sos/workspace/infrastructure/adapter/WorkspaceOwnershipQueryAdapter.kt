package pizza.psycho.sos.workspace.infrastructure.adapter

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.identity.account.application.service.WorkspaceOwnershipQueryService
import pizza.psycho.sos.workspace.domain.repository.WorkspaceMembershipQueryRepository
import java.util.UUID

@Component
class WorkspaceOwnershipQueryAdapter(
    private val workspaceMembershipQueryRepository: WorkspaceMembershipQueryRepository,
) : WorkspaceOwnershipQueryService {
    @Transactional(readOnly = true)
    override fun existsActiveOwnerMembershipByAccountId(accountId: UUID): Boolean =
        workspaceMembershipQueryRepository.existsActiveOwnerMembershipByAccountId(accountId)
}
