package pizza.psycho.sos.project.task.application.service.dto

import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.project.task.domain.model.vo.Priority
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

sealed interface TaskCommand {
    data class AddTask(
        val workspaceId: UUID,
        val title: String,
        val description: String,
        val assigneeId: UUID?,
        val dueDate: Instant?,
    ) : TaskCommand

    data class RemoveTask(
        val workspaceId: UUID,
        val id: UUID,
        val deletedBy: UUID,
    ) : TaskCommand

    data class UpdateTask(
        val workspaceId: UUID,
        val id: UUID,
        val title: Patch<String> = Patch.Undefined,
        val description: Patch<String> = Patch.Undefined,
        val status: Patch<Status> = Patch.Undefined,
        val assigneeId: Patch<UUID> = Patch.Undefined,
        val dueDate: Patch<Instant> = Patch.Undefined,
        val priority: Patch<Priority> = Patch.Undefined,
        val actorId: UUID?,
    ) : TaskCommand
}
