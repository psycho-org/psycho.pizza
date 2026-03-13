package pizza.psycho.sos.project.sprint.application.port.out.dto

import java.time.Instant
import java.util.UUID

/**
 * SprintPort 를 통해 노출되는 Sprint 읽기용 스냅샷.
 */
data class SprintSnapshot(
    val sprintId: UUID,
    val workspaceId: UUID,
    val name: String,
    val goal: String?,
    val startDate: Instant,
    val endDate: Instant,
    val projectIds: List<UUID>,
)
