package pizza.psycho.sos.project.project.application.service.dto

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import java.time.Instant
import java.util.UUID

sealed interface ProjectCommand {
    data class Create(
        val workspaceId: WorkspaceId,
        val name: String,
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

    data class CreateTask(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
        val title: String,
        val description: String,
        val assigneeId: UUID? = null,
        val dueDate: Instant? = null,
    )

    /**
     * Task를 한 프로젝트에서 다른 프로젝트로 이동시키는 커맨드
     */
    data class MoveTask(
        val workspaceId: WorkspaceId,
        val fromProjectId: UUID,
        val toProjectId: UUID,
        val taskId: UUID,
        val movedBy: UUID,
    )
}
