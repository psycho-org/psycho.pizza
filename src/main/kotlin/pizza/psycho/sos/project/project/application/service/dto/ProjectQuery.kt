package pizza.psycho.sos.project.project.application.service.dto

import org.springframework.data.domain.Pageable
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import java.util.UUID

sealed interface ProjectQuery {
    data class Find(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
    )

    data class FindTasksInProject(
        val workspaceId: WorkspaceId,
        val projectId: UUID,
        val pageable: Pageable,
    )
}
