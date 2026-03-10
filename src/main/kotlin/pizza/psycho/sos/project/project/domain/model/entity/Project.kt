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
import pizza.psycho.sos.common.event.DomainEventDelegate
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import java.util.UUID

@Entity
@Table(name = "projects")
class Project(
    @Column(name = "name", nullable = false)
    var name: String,
    @Embedded
    val workspaceId: WorkspaceId,
) : BaseDeletableEntity(),
    AggregateRoot by DomainEventDelegate() {
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    private val mappings: MutableSet<ProjectTaskMapping> = mutableSetOf()

    val projectId: UUID
        get() = id ?: throw DomainException("Project ID is null")

    init {
        modify(name)
    }

    fun modify(name: String) {
        if (name.isBlank()) {
            throw DomainException("Project name is blank")
        }
        this.name = name
    }

    fun addTask(taskId: UUID) {
        if (mappings.none { it.taskId == taskId }) {
            mappings += ProjectTaskMapping(project = this, taskId = taskId, workspaceId = this.workspaceId)
        }
    }

    fun addTasks(taskIds: Collection<UUID>) {
        taskIds.forEach { addTask(it) }
    }

    fun removeTasks(taskIds: Collection<UUID>) {
        taskIds.forEach { removeTask(it) }
    }

    fun removeTask(taskId: UUID) {
        mappings.removeIf { it.taskId == taskId }
    }

    fun hasTask(taskId: UUID): Boolean = mappings.any { it.taskId == taskId }

    fun taskIds(): List<UUID> = mappings.map { it.taskId }

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
}
