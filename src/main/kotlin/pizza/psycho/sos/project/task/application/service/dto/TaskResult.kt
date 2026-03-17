package pizza.psycho.sos.project.task.application.service.dto

import org.springframework.data.domain.Page
import pizza.psycho.sos.project.task.domain.model.vo.Priority
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

sealed interface TaskResult {
    data class TaskInformation(
        val id: UUID,
        val title: String,
        val description: String,
        val status: Status,
        val priority: Priority? = null,
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

    data class AssignedTaskList(
        val page: Page<AssignedTaskListInfo>,
    ) : TaskResult

    data class TaskListInfo(
        val id: UUID,
        val title: String,
        val status: Status,
        val assignee: Assignee? = null,
        val dueDate: Instant? = null,
    )

    data class AssignedTaskListInfo(
        val id: UUID,
        val title: String,
        val status: Status,
        val assignee: Assignee? = null,
        val dueDate: Instant? = null,
        val projects: List<Project>,
        val sprints: List<Sprint>,
    )

    data class Project(
        val id: UUID,
        val name: String,
    )

    data class Sprint(
        val id: UUID,
        val name: String,
        val startDate: Instant,
        val endDate: Instant,
    )

    data class Remove(
        val count: Int,
    ) : TaskResult

    sealed interface Failure : TaskResult {
        data object IdNotFound : Failure

        data object TaskInformationNotFound : Failure

        data object InvalidRequest : Failure
    }
}
