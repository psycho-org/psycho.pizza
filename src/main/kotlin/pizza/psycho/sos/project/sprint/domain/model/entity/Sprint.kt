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
import pizza.psycho.sos.common.event.DomainEvent
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.domain.event.SprintDomainEvent
import pizza.psycho.sos.project.sprint.domain.event.SprintGoalChangedEvent
import pizza.psycho.sos.project.sprint.domain.event.SprintPeriodChangedEvent
import pizza.psycho.sos.project.sprint.domain.exception.SprintErrorCode
import pizza.psycho.sos.project.sprint.domain.model.vo.Period
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sprints")
class Sprint(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "goal")
    var goal: String? = null,
    @Embedded
    val workspaceId: WorkspaceId,
    @Embedded
    var period: Period,
) : BaseDeletableEntity(),
    AggregateRoot {
    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    private val mappings: MutableSet<SprintProjectMapping> = mutableSetOf()

    val sprintId: UUID
        get() = id ?: throw DomainException(SprintErrorCode.SPRINT_ID_NOT_FOUND, "Sprint ID is null")

    init {
        modify(name)
    }

    fun modify(name: String) {
        if (name.isBlank()) {
            throw DomainException(SprintErrorCode.SPRINT_NAME_NOT_VALID)
        }
        this.name = name
    }

    fun changeGoal(
        goal: String? = null,
        by: UUID,
    ) {
        val old = this.goal
        if (old == goal) {
            return
        }
        requireGoalNotBlank(goal)

        this.goal = goal

        SprintGoalChangedEvent(
            workspaceId = this.workspaceId.value,
            actorId = by,
            sprintId = this.sprintId,
            fromGoal = old ?: "",
            toGoal = goal ?: "",
            eventId = UUID.randomUUID(),
        ).register()
    }

    fun changePeriod(
        startDate: Instant? = null,
        endDate: Instant? = null,
        by: UUID,
    ) {
        val previousPeriod = this.period
        val nextPeriod =
            previousPeriod.copy(
                startDate = startDate ?: previousPeriod.startDate,
                endDate = endDate ?: previousPeriod.endDate,
            )
        if (previousPeriod == nextPeriod) {
            return
        }

        this.period = nextPeriod

        SprintPeriodChangedEvent(
            workspaceId = workspaceId.value,
            sprintId = sprintId,
            actorId = by,
            fromPeriod = previousPeriod.toString(),
            toPeriod = period.toString(),
            eventId = UUID.randomUUID(),
        ).register()
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

    private fun SprintDomainEvent.register() = registerEvent(this)

    companion object {
        fun create(
            name: String,
            workspaceId: WorkspaceId,
            goal: String?,
            startDate: Instant,
            endDate: Instant,
        ): Sprint {
            requireGoalNotBlank(goal)
            return Sprint(
                name = name,
                goal = goal,
                workspaceId = workspaceId,
                period = Period(startDate, endDate),
            )
        }

        private fun requireGoalNotBlank(goal: String?) {
            goal?.let {
                if (it.isBlank()) {
                    throw DomainException(SprintErrorCode.GOAL_NOT_EMPTY_OR_BLANK)
                }
                if (it.length > 120) {
                    throw DomainException(SprintErrorCode.GOAL_LENGTH_TOO_LONG)
                }
            }
        }
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
