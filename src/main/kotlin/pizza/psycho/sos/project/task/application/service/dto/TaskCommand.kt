package pizza.psycho.sos.project.task.application.service.dto

import org.springframework.data.domain.Pageable
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

    data class FindTasks(
        val workspaceId: UUID,
        val pageable: Pageable,
    ) : TaskCommand

    data class FindTask(
        val workspaceId: UUID,
        val id: UUID,
    ) : TaskCommand
}
