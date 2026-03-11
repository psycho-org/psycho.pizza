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
import pizza.psycho.sos.common.patch.Patch
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.event.TaskAssigneeChangedEvent
import pizza.psycho.sos.project.task.domain.event.TaskDeletedEvent
import pizza.psycho.sos.project.task.domain.event.TaskDomainEvent
import pizza.psycho.sos.project.task.domain.event.TaskDueDateChangedEvent
import pizza.psycho.sos.project.task.domain.event.TaskStatusChangedEvent
import pizza.psycho.sos.project.task.domain.exception.InvalidPriorityTransitionException
import pizza.psycho.sos.project.task.domain.exception.InvalidStatusTransitionException
import pizza.psycho.sos.project.task.domain.model.vo.AssigneeId
import pizza.psycho.sos.project.task.domain.model.vo.Priority
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
    @Enumerated(EnumType.STRING)
    var priority: Priority? = null,
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

    fun assign(
        assigneeId: UUID,
        by: UUID? = null,
    ) = changeAssignee(assigneeId, by)

    fun unassign(by: UUID) = changeAssignee(null, by)

    private fun changeAssignee(
        newAssigneeId: UUID?,
        by: UUID? = null,
    ) {
        val old = this.assigneeId
        this.assigneeId = AssigneeId(newAssigneeId)

        TaskAssigneeChangedEvent(
            workspaceId = this.workspaceId.value,
            actorId = by,
            taskId = this.taskId,
            fromAssigneeId = old.value?.toString(),
            toAssigneeId = this.assigneeId.value?.toString(),
            eventId = UUID.randomUUID(),
        ).register()
    }

    fun changePriority(
        priority: Priority,
        by: UUID? = null,
    ) {
        this.priority?.let {
            if (!it.isTransitionableTo(priority)) {
                throw InvalidPriorityTransitionException(from = it, to = priority)
            }
        }

        this.priority = priority
    }

    fun changeStatus(
        status: Status,
        by: UUID? = null,
    ) {
        if (!this.status.isTransitionableTo(status)) {
            throw InvalidStatusTransitionException(from = this.status, to = status)
        }

        val old = this.status
        this.status = status

        TaskStatusChangedEvent(
            workspaceId = this.workspaceId.value,
            actorId = by,
            taskId = this.taskId,
            fromStatus = old.name,
            toStatus = this.status.name,
            eventId = UUID.randomUUID(),
        ).register()
    }

    fun changeDueDate(
        dueDate: Instant,
        by: UUID? = null,
    ) {
        val old = this.dueDate
        this.dueDate = TaskDueDate.withValidation(dueDate)

        TaskDueDateChangedEvent(
            workspaceId = this.workspaceId.value,
            actorId = by,
            taskId = this.taskId,
            fromDueDate = old.value,
            toDueDate = this.dueDate.value,
            eventId = UUID.randomUUID(),
        ).register()
    }

    fun clearDueDate() {
        this.dueDate = TaskDueDate()
    }

    fun apply(spec: TaskUpdateSpec) {
        // 1) 제목 / 설명
        val newTitle = (spec.title as? Patch.Value)?.value
        val newDescription = (spec.description as? Patch.Value)?.value
        if (newTitle != null || newDescription != null) {
            modify(title = newTitle, description = newDescription)
        }

        // 2) 상태 (이벤트 발생)
        when (spec.status) {
            is Patch.Value -> {
                val newStatus = spec.status.value
                if (status != newStatus) {
                    changeStatus(newStatus, spec.actorId)
                }
            }

            Patch.Undefined, Patch.Clear -> Unit
        }

        // 3) 담당자 (이벤트 발생)
        val currentAssigneeId = assigneeId.value
        when (spec.assigneeId) {
            is Patch.Value -> {
                val newAssigneeId = spec.assigneeId.value
                if (newAssigneeId != currentAssigneeId) {
                    assign(newAssigneeId, spec.actorId)
                }
            }

            Patch.Clear -> {
                if (currentAssigneeId != null) {
                    unassign(spec.actorId ?: UUID.randomUUID())
                }
            }

            Patch.Undefined -> Unit
        }

        // 4) 마감일 (set 시 이벤트, clear 는 현재 도메인 정책상 이벤트 없음)
        val currentDueDate = dueDate.value
        when (spec.dueDate) {
            is Patch.Value -> {
                val newDueDate = spec.dueDate.value
                if (newDueDate != currentDueDate) {
                    changeDueDate(newDueDate, spec.actorId)
                }
            }

            Patch.Clear -> {
                if (currentDueDate != null) {
                    clearDueDate()
                }
            }

            Patch.Undefined -> Unit
        }

        // 5) 우선순위 (이벤트는 아직 없음)
        when (spec.priority) {
            is Patch.Value -> {
                val newPriority = spec.priority.value
                if (priority != newPriority) {
                    changePriority(newPriority, spec.actorId)
                }
            }

            Patch.Clear -> {
                if (priority != null) {
                    priority = null
                }
            }

            Patch.Undefined -> Unit
        }
    }

    override fun delete(by: UUID) {
        super.delete(by)
        TaskDeletedEvent(
            workspaceId = this.workspaceId.value,
            actorId = by,
            taskId = this.taskId,
            taskTitle = this.title,
            eventId = UUID.randomUUID(),
        ).register()
    }

    @PostLoad
    private fun ensureNonNullFields() {
        // @Embedded 필드는 모든 컬럼이 null이면 Hibernate가 객체 자체를 null로 세팅함
        // Kotlin non-null 타입과 충돌하므로 @PostLoad에서 방어적으로 초기화
        assigneeId = assigneeId ?: AssigneeId.empty()
        dueDate = dueDate ?: TaskDueDate()
    }

    private fun TaskDomainEvent.register() = registerEvent(this)

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
