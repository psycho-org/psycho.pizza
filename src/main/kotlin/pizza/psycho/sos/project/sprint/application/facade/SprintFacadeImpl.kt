package pizza.psycho.sos.project.sprint.application.facade

import org.springframework.stereotype.Service
import pizza.psycho.sos.common.support.transaction.helper.Tx
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.application.port.out.dto.SprintSnapshot
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import java.util.UUID

@Service
class SprintFacadeImpl(
    private val sprintRepository: SprintRepository,
) : SprintFacade {
    override fun findSprintByIdWithProjects(
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): SprintSnapshot? =
        Tx.readable {
            val sprint = sprintRepository.findActiveSprintByIdOrNull(sprintId, workspaceId) ?: return@readable null

            SprintSnapshot(
                sprintId = sprint.sprintId,
                workspaceId = sprint.workspaceId.value,
                name = sprint.name,
                goal = sprint.goal,
                startDate = sprint.period.startDate,
                endDate = sprint.period.endDate,
                projectIds = sprint.projectIds(),
            )
        }
}
