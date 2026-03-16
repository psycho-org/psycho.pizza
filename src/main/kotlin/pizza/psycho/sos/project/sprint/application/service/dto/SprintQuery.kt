package pizza.psycho.sos.project.sprint.application.service.dto

import org.springframework.data.domain.Pageable
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.util.UUID

sealed interface SprintQuery {
    data class Find(
        val workspaceId: WorkspaceId,
        val sprintId: UUID,
    )

    data class FindTasksInSprint(
        val workspaceId: WorkspaceId,
        val sprintId: UUID,
        val status: Status,
    )

    data class FindProjectsInSprint(
        val workspaceId: WorkspaceId,
        val sprintId: UUID,
    )

    data class FindAll(
        val workspaceId: WorkspaceId,
        val pageable: Pageable,
    )
}
