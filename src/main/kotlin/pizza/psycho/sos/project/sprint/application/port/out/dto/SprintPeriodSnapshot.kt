package pizza.psycho.sos.project.sprint.application.port.out.dto

import java.time.Instant
import java.util.UUID

data class SprintPeriodSnapshot(
    val sprintId: UUID,
    val workspaceId: UUID,
    val startDate: Instant,
    val endDate: Instant,
)
