package pizza.psycho.sos.project.sprint.domain.model.entity

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
import pizza.psycho.sos.project.sprint.domain.model.vo.Period
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sprints")
class Sprint(
    @Column(name = "name", nullable = false)
    var name: String,
    @Embedded
    val workspaceId: WorkspaceId,
    @Embedded
    var period: Period,
) : BaseDeletableEntity(),
    AggregateRoot by DomainEventDelegate() {
    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    private val mappings: MutableSet<SprintProjectMapping> = mutableSetOf()

    val sprintId: UUID
        get() = id ?: throw DomainException("Sprint ID is null")

    init {
        modify(name)
    }

    fun modify(name: String) {
        if (name.isBlank()) {
            throw DomainException("Sprint name is blank")
        }
        this.name = name
    }

    fun changePeriod(
        startDate: Instant? = null,
        endDate: Instant? = null,
    ) {
        this.period =
            period.copy(
                startDate = startDate ?: period.startDate,
                endDate = endDate ?: period.endDate,
            )
    }

    fun addProject(projectId: UUID) {
        if (mappings.none { it.projectId == projectId }) {
            mappings += SprintProjectMapping(sprint = this, projectId = projectId, workspaceId = workspaceId)
        }
    }

    fun addProjects(projectIds: Collection<UUID>) {
        projectIds.forEach { addProject(it) }
    }

    fun removeProject(projectId: UUID) {
        mappings.removeIf { it.projectId == projectId }
    }

    fun removeProjects(projectIds: Collection<UUID>) {
        projectIds.forEach { removeProject(it) }
    }

    fun hasProject(projectId: UUID): Boolean = mappings.any { it.projectId == projectId }

    fun projectIds(): List<UUID> = mappings.map { it.projectId }

    companion object {
        fun create(
            name: String,
            workspaceId: WorkspaceId,
            startDate: Instant,
            endDate: Instant,
        ) = Sprint(
            name = name,
            workspaceId = workspaceId,
            period = Period(startDate, endDate),
        )
    }
}
