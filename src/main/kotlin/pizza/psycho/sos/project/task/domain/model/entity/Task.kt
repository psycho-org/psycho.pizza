package pizza.psycho.sos.project.task.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseDeletableEntity
import pizza.psycho.sos.common.event.AggregateRoot
import pizza.psycho.sos.common.event.DomainEventDelegate
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.model.vo.AssigneeId
import pizza.psycho.sos.project.task.domain.model.vo.Status
import pizza.psycho.sos.project.task.domain.model.vo.TaskDueDate
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tasks")
class Task protected constructor(
    @Column(name = "title", nullable = false, length = 512)
    var title: String,
    @Column(name = "description", nullable = false)
    var description: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: Status = Status.TODO,
    @Embedded
    var assigneeId: AssigneeId = AssigneeId.empty(),
    @Embedded
    val workspaceId: WorkspaceId,
    @Embedded
    var dueDate: TaskDueDate = TaskDueDate(),
) : BaseDeletableEntity(),
    AggregateRoot by DomainEventDelegate() {
    fun modify(
        title: String? = null,
        description: String? = null,
    ) {
        this.title = title ?: this.title
        this.description = description ?: this.description
    }

    fun assign(assigneeId: UUID) {
        this.assigneeId = AssigneeId(assigneeId)
    }

    fun unassign() {
        this.assigneeId = AssigneeId.empty()
    }

    fun changeStatus(status: Status) {
        this.status = status
    }

    fun changeDueDate(dueDate: Instant) {
        this.dueDate = TaskDueDate.withValidation(dueDate)
    }

    fun clearDueDate() {
        this.dueDate = TaskDueDate()
    }

    companion object {
        fun create(
            title: String,
            description: String,
            assigneeId: UUID? = null,
            workspaceId: UUID,
            dueDate: Instant? = null,
        ): Task =
            Task(
                title = title,
                description = description,
                assigneeId = AssigneeId(assigneeId),
                workspaceId = WorkspaceId(workspaceId),
                dueDate = TaskDueDate.withValidation(dueDate),
            )
    }
}
