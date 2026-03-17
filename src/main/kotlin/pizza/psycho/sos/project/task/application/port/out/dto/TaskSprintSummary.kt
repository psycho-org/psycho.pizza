package pizza.psycho.sos.project.task.application.port.out.dto

import java.time.Instant
import java.util.UUID

data class TaskSprintSummary(
    val id: UUID,
    val name: String,
    val startDate: Instant,
    val endDate: Instant,
)
