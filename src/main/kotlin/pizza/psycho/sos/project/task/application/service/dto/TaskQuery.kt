package pizza.psycho.sos.project.task.application.service.dto

import org.springframework.data.domain.Pageable
import java.util.UUID

sealed interface TaskQuery {
    data class FindTasks(
        val workspaceId: UUID,
        val pageable: Pageable,
    ) : TaskQuery

    data class FindBacklogTasks(
        val workspaceId: UUID,
        val pageable: Pageable,
    ) : TaskQuery

    data class FindTask(
        val workspaceId: UUID,
        val id: UUID,
    ) : TaskQuery
}
