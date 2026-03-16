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
        val reason: String,
    ) : TaskCommand

    data class UpdateTask(
        val workspaceId: UUID,
        val id: UUID,
        val title: Patch<String> = Patch.Unchanged,
        val description: Patch<String> = Patch.Unchanged,
        val status: Patch<Status> = Patch.Unchanged,
        val assigneeId: Patch<UUID> = Patch.Unchanged,
        val dueDate: Patch<Instant> = Patch.Unchanged,
        val priority: Patch<Priority> = Patch.Unchanged,
        val actorId: UUID?,
    ) : TaskCommand
}
