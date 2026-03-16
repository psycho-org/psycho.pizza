package pizza.psycho.sos.project.sprint.application.service.dto

import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import java.time.Instant
import java.util.UUID

sealed interface SprintCommand {
    data class Create(
        val workspaceId: WorkspaceId,
        val name: String,
        val goal: String?,
        val startDate: Instant,
        val endDate: Instant,
    )

    data class Update(
        val workspaceId: WorkspaceId,
        val sprintId: UUID,
        val name: String? = null,
        val goal: Patch<String> = Patch.Unchanged,
        val startDate: Instant? = null,
        val endDate: Instant? = null,
        val addProjectIds: List<UUID> = emptyList(),
        val removeProjectIds: List<UUID> = emptyList(),
        val by: UUID,
    )

    data class Remove(
        val workspaceId: WorkspaceId,
        val sprintId: UUID,
        val deletedBy: UUID,
    ) : SprintCommand

    data class CreateProject(
        val workspaceId: WorkspaceId,
        val sprintId: UUID,
        val name: String,
    ) : SprintCommand
}
