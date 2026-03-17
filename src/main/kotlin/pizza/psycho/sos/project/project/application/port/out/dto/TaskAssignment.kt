package pizza.psycho.sos.project.project.application.port.out.dto

import java.util.UUID

data class TaskAssignment(
    val taskId: UUID,
    val projectId: UUID,
)
