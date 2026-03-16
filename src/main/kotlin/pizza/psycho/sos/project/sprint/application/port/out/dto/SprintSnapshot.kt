package pizza.psycho.sos.project.sprint.application.port.out.dto

import java.time.Instant
import java.util.UUID

data class SprintSnapshot(
    val sprintId: UUID,
    val workspaceId: UUID,
    val name: String,
    val goal: String?,
    val startDate: Instant,
    val endDate: Instant,
    val projectIds: List<UUID>,
)
