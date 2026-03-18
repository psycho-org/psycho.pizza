package pizza.psycho.sos.workspace.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.workspace.application.port.WorkspaceMembershipExistencePort
import pizza.psycho.sos.workspace.domain.repository.WorkspaceMembershipQueryRepository
import java.util.UUID

@Service
class WorkspaceMembershipExistenceService(
    private val workspaceMembershipQueryRepository: WorkspaceMembershipQueryRepository,
) : WorkspaceMembershipExistencePort {
    @Transactional(readOnly = true)
    override fun existsActiveMembership(
        workspaceId: UUID,
        accountId: UUID,
    ): Boolean = workspaceMembershipQueryRepository.existsActiveMembershipByWorkspaceIdAndAccountId(workspaceId, accountId)
}
