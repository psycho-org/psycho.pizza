package pizza.psycho.sos.project.project.domain.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseDeletableEntity
import pizza.psycho.sos.common.event.AggregateRoot
import pizza.psycho.sos.common.event.DomainEvent
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.domain.event.ProjectDeletedEvent
import pizza.psycho.sos.project.project.domain.event.ProjectDomainEvent
import pizza.psycho.sos.project.project.domain.event.TaskAddedToProjectEvent
import pizza.psycho.sos.project.project.domain.event.TaskProjectChangedEvent
import pizza.psycho.sos.project.project.domain.event.TaskRemovedFromProjectEvent
import pizza.psycho.sos.project.project.domain.event.TasksAddedToProjectEvent
import pizza.psycho.sos.project.project.domain.event.TasksRemovedFromProjectEvent
import pizza.psycho.sos.project.project.domain.exception.ProjectErrorCode
import java.util.UUID

@Entity
@Table(name = "projects")
class Project(
    @Column(name = "name", nullable = false)
    var name: String,
    @Embedded
    val workspaceId: WorkspaceId,
) : BaseDeletableEntity(),
    AggregateRoot {
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    private val mappings: MutableSet<ProjectTaskMapping> = mutableSetOf()

    val projectId: UUID
        get() = id ?: throw DomainException(ProjectErrorCode.PROJECT_ID_NULL)

    init {
        modify(name)
    }

    fun modify(name: String) {
        if (name.isBlank()) {
            throw DomainException(ProjectErrorCode.PROJECT_NAME_NULL)
        }
        this.name = name
    }

    fun addTask(
        taskId: UUID,
        by: UUID? = null,
    ) {
        if (mappings.none { it.taskId == taskId }) {
            mappings += ProjectTaskMapping(project = this, taskId = taskId, workspaceId = this.workspaceId)

            TaskAddedToProjectEvent(
                workspaceId = this.workspaceId.value,
                actorId = by,
                taskId = taskId,
                projectId = this.projectId,
                eventId = UUID.randomUUID(),
            ).register()
        }
    }

    fun addTasks(
        taskIds: Collection<UUID>,
        by: UUID? = null,
    ) {
        val addedTaskIds =
            taskIds
                .distinct()
                .filterNot(this::hasTask)

        addedTaskIds.forEach { taskId ->
            mappings += ProjectTaskMapping(project = this, taskId = taskId, workspaceId = this.workspaceId)
        }

        when (addedTaskIds.size) {
            0 -> Unit
            1 ->
                TaskAddedToProjectEvent(
                    workspaceId = this.workspaceId.value,
                    actorId = by,
                    taskId = addedTaskIds.single(),
                    projectId = this.projectId,
                    eventId = UUID.randomUUID(),
                ).register()

            else ->
                TasksAddedToProjectEvent(
                    workspaceId = this.workspaceId.value,
                    actorId = by,
                    taskIds = addedTaskIds,
                    projectId = this.projectId,
                    eventId = UUID.randomUUID(),
                ).register()
        }
    }

    fun removeTasks(
        taskIds: Collection<UUID>,
        by: UUID? = null,
    ) {
        val removedTaskIds =
            taskIds
                .distinct()
                .filter { taskId -> mappings.removeIf { it.taskId == taskId } }

        when (removedTaskIds.size) {
            0 -> Unit
            1 ->
                TaskRemovedFromProjectEvent(
                    workspaceId = this.workspaceId.value,
                    actorId = by,
                    taskId = removedTaskIds.single(),
                    projectId = this.projectId,
                    eventId = UUID.randomUUID(),
                ).register()

            else ->
                TasksRemovedFromProjectEvent(
                    workspaceId = this.workspaceId.value,
                    actorId = by,
                    taskIds = removedTaskIds,
                    projectId = this.projectId,
                    eventId = UUID.randomUUID(),
                ).register()
        }
    }

    fun removeTask(
        taskId: UUID,
        by: UUID? = null,
    ) {
        val removed = mappings.removeIf { it.taskId == taskId }

        if (removed) {
            TaskRemovedFromProjectEvent(
                workspaceId = this.workspaceId.value,
                actorId = by,
                taskId = taskId,
                projectId = this.projectId,
                eventId = UUID.randomUUID(),
            ).register()
        }
    }

    fun moveTaskTo(
        taskId: UUID,
        to: Project,
        by: UUID,
    ) {
        if (!this.hasTask(taskId)) {
            throw DomainException(ProjectErrorCode.TASK_NOT_FOUND)
        }
        if (this.projectId == to.projectId) {
            throw DomainException(ProjectErrorCode.SAME_PROJECT)
        }
        this.removeTask(taskId, by)
        to.addTask(taskId, by)

        TaskProjectChangedEvent(
            workspaceId = this.workspaceId.value,
            actorId = by,
            taskId = taskId,
            fromProjectId = this.projectId,
            toProjectId = to.projectId,
            eventId = UUID.randomUUID(),
        ).register()
    }

    fun hasTask(taskId: UUID): Boolean = mappings.any { it.taskId == taskId }

    fun taskIds(): List<UUID> = mappings.map { it.taskId }

    override fun delete(by: UUID) {
        delete(by, null)
    }

    fun delete(
        by: UUID,
        reason: String?,
    ) {
        super.delete(by)
        ProjectDeletedEvent(
            workspaceId = workspaceId.value,
            actorId = by,
            projectId = projectId,
            projectName = name,
            reason = reason,
            eventId = UUID.randomUUID(),
        ).register()
    }

    private fun ProjectDomainEvent.register() = registerEvent(this)

    companion object {
        fun create(
            name: String,
            workspaceId: WorkspaceId,
        ): Project =
            Project(
                name = name,
                workspaceId = workspaceId,
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
