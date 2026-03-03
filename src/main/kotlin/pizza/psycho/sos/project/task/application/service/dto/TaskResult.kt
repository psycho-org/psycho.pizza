package pizza.psycho.sos.project.task.application.service.dto

import org.springframework.data.domain.Page
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

sealed interface TaskResult {
    data object Success : TaskResult

    data class TaskInformation(
        val id: UUID,
        val title: String,
        val description: String,
        val status: Status,
        val assignee: Assignee? = null,
        val workspaceId: UUID,
        val dueDate: Instant?,
    ) : TaskResult

    data class Assignee(
        val id: UUID,
        val name: String,
        val email: String,
    )

    data class TaskList(
        val page: Page<TaskListInfo>,
    ) : TaskResult

    data class TaskListInfo(
        val id: UUID,
        val title: String,
        val assignee: Assignee? = null,
        val dueDate: Instant? = null,
    )

    sealed interface Failure : TaskResult {
        data object IdNotFound : Failure

        data object TaskInformationNotFound : Failure
    }
}
