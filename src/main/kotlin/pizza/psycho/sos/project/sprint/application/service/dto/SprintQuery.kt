package pizza.psycho.sos.project.sprint.application.service.dto

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import java.util.UUID

sealed interface SprintQuery {
    data class Find(
        val workspaceId: WorkspaceId,
        val sprintId: UUID,
    )

    data class FindProjectsInSprint(
        val workspaceId: WorkspaceId,
        val sprintId: UUID,
    )
}
