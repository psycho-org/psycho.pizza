package pizza.psycho.sos.project.sprint.application.facade

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.application.port.out.dto.SprintSnapshot
import java.util.UUID

interface SprintFacade {
    fun findSprintByIdWithProjects(
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): SprintSnapshot?
}
