package pizza.psycho.sos.project.task.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.PostLoad
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseDeletableEntity
import pizza.psycho.sos.common.event.AggregateRoot
import pizza.psycho.sos.common.event.DomainEvent
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.model.vo.AssigneeId
import pizza.psycho.sos.project.task.domain.model.vo.Status
import pizza.psycho.sos.project.task.domain.model.vo.TaskDueDate
import java.time.Instant
import java.util.UUID

/*
 * todo: task의 우선순위 추가
 */
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
    AggregateRoot {
    val taskId: UUID
        get() = id ?: throw DomainException("Task ID is null")

    init {
        changeTitle(title)
        changeDescription(description)
    }

    fun modify(
        title: String? = null,
        description: String? = null,
    ) {
        title?.let { changeTitle(it) }
        description?.let { changeDescription(it) }
    }

    private fun changeTitle(title: String) {
        if (title.isBlank()) {
            throw DomainException("Title cannot be blank")
        }
        this.title = title
    }

    private fun changeDescription(description: String) {
        if (description.isBlank()) {
            throw DomainException("Description cannot be blank")
        }
        this.description = description
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

    @PostLoad
    private fun ensureNonNullFields() {
        // @Embedded 필드는 모든 컬럼이 null이면 Hibernate가 객체 자체를 null로 세팅함
        // Kotlin non-null 타입과 충돌하므로 @PostLoad에서 방어적으로 초기화
        assigneeId = assigneeId ?: AssigneeId.empty()
        dueDate = dueDate ?: TaskDueDate()
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

    // 이벤트 관련 ---------------------------------------------------------------------

    @Transient
    private var events: MutableSet<DomainEvent>? = null

    private fun ensureEvents(): MutableSet<DomainEvent> {
        if (events == null) {
            events = mutableSetOf()
        }
        return events!!
    }

    override fun registerEvent(event: DomainEvent) {
        ensureEvents() += event
    }

    override fun domainEvents(): List<DomainEvent> = events?.toList() ?: emptyList()

    override fun pullDomainEvents(): List<DomainEvent> = domainEvents().also { events?.clear() }
}
