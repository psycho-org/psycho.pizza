package pizza.psycho.sos.project.project.application.port.out.query

import java.util.UUID

data class ProjectProgress(
    val projectId: UUID,
    val totalCount: Long,
    val completedCount: Long,
) {
    val value: Double
        get() = if (totalCount == 0L) 0.0 else completedCount.toDouble() / totalCount
}
