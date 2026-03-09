package pizza.psycho.sos.project.project.application.service.dto

import org.springframework.data.domain.Pageable
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import java.util.UUID

sealed interface ProjectCommand {
    data class Create(
        val workspaceId: WorkspaceId,
        val name: String,
    )

    data class Get(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
    )

    data class GetTasks(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
        val pageable: Pageable,
    )

    data class Update(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
        val name: String?,
        val addTaskIds: List<UUID> = emptyList(),
        val removeTaskIds: List<UUID> = emptyList(),
    )

    data class Remove(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
        val deletedBy: UUID,
    )

    data class RemoveWithTasks(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
        val deletedBy: UUID,
    )
}
