package pizza.psycho.sos.project.sprint.infrastructure.persistence.adapter

import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.application.facade.SprintFacade
import pizza.psycho.sos.project.sprint.application.port.out.SprintPort
import pizza.psycho.sos.project.sprint.application.port.out.dto.SprintSnapshot
import java.util.UUID

@Component
class SprintAdapter(
    private val sprintFacade: SprintFacade,
) : SprintPort {
    override fun findByIdWithProjects(
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): SprintSnapshot? = sprintFacade.findSprintByIdWithProjects(sprintId, workspaceId)
}
