package pizza.psycho.sos.project.task.presentation.dto

import pizza.psycho.sos.common.response.OffsetPageInfo
import pizza.psycho.sos.project.task.domain.model.vo.Priority
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

sealed interface TaskResponse {
    data class Information(
        val id: UUID,
        val title: String,
        val description: String,
        val status: Status,
        val priority: Priority? = null,
        val assignee: Assignee? = null,
        val workspaceId: UUID,
        val dueDate: Instant?,
    ) : TaskResponse

    data class List(
        val id: UUID,
        val title: String,
        val status: Status,
        val assignee: Assignee? = null,
        val dueDate: Instant? = null,
    ) : TaskResponse

    data class AssignedGrouped(
        val pageInfo: OffsetPageInfo,
        val sprintGroups: kotlin.collections.List<SprintGroup>,
    ) : TaskResponse

    data class Assignee(
        val id: UUID,
        val name: String,
        val email: String,
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

    data class SprintGroup(
        val sprint: Sprint? = null,
        val uniqueTaskCount: Int,
        val projects: kotlin.collections.List<ProjectGroup>,
    )

    data class ProjectGroup(
        val project: Project? = null,
        val taskCount: Int,
        val tasks: kotlin.collections.List<AssignedTask>,
    )

    data class AssignedTask(
        val id: UUID,
        val title: String,
        val status: Status,
        val assignee: Assignee? = null,
        val dueDate: Instant? = null,
    )

    data class Remove(
        val count: Int,
    )
}
