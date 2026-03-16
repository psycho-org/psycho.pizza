package pizza.psycho.sos.project.task.application.port.out.dto

import pizza.psycho.sos.project.task.domain.model.vo.Priority
import pizza.psycho.sos.project.task.domain.model.vo.Status
import java.time.Instant
import java.util.UUID

data class TaskSnapshot(
    val id: UUID,
    val title: String,
    val status: Status,
    val priority: Priority? = null,
    val assigneeId: UUID? = null,
    val dueDate: Instant? = null,
)
