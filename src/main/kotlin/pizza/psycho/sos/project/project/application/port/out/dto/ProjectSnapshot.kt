package pizza.psycho.sos.project.project.application.port.out.dto

import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import java.util.UUID

data class ProjectSnapshot(
    val projectId: UUID,
    val workspaceId: WorkspaceId,
    val name: String,
    val taskIds: List<UUID>,
)
