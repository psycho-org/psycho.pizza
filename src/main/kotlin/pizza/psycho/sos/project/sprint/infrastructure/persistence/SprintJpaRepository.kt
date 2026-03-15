package pizza.psycho.sos.project.sprint.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.sprint.domain.model.entity.Sprint
import pizza.psycho.sos.project.sprint.domain.repository.SprintRepository
import java.util.UUID

@Component
interface SprintJpaRepository :
    SprintRepository,
    JpaRepository<Sprint, UUID> {
    override fun findActiveSprintByIdOrNull(
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): Sprint? = findByIdAndWorkspaceIdValueAndDeletedAtIsNull(sprintId, workspaceId.value)

    override fun deleteById(
        sprintId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int =
        findByIdAndWorkspaceIdValueAndDeletedAtIsNull(sprintId, workspaceId.value)
            ?.also { it.delete(deletedBy) }
            ?.let { 1 }
            ?: 0

    @Query(
        """
        select case when count(s) > 0 then true else false end
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
            join pizza.psycho.sos.project.project.domain.model.entity.ProjectTaskMapping ptm
                on ptm.project = p
            join pizza.psycho.sos.project.task.domain.model.entity.Task t
                on t.id = ptm.taskId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and t.deletedAt is null
          and ptm.taskId = :taskId
        """,
    )
    override fun existsActiveSprintByTaskId(
        taskId: UUID,
        workspaceId: WorkspaceId,
    ): Boolean

    @Query(
        """
        select distinct s.id
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
            join pizza.psycho.sos.project.project.domain.model.entity.ProjectTaskMapping ptm
                on ptm.project = p
            join pizza.psycho.sos.project.task.domain.model.entity.Task t
                on t.id = ptm.taskId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and t.deletedAt is null
          and ptm.taskId = :taskId
        """,
    )
    override fun findActiveSprintIdsByTaskId(
        taskId: UUID,
        workspaceId: WorkspaceId,
    ): List<UUID>

    override fun findActiveSprintIdsByTaskIds(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): Map<UUID, Set<UUID>> =
        if (taskIds.isEmpty()) {
            emptyMap()
        } else {
            findTaskSprintRows(taskIds, workspaceId)
                .groupBy(
                    keySelector = { row: TaskSprintRow -> row.getTaskId() },
                    valueTransform = { row: TaskSprintRow -> row.getSprintId() },
                ).mapValues { entry -> entry.value.toSet() }
        }

    @Query(
        """
        select case when count(s) > 0 then true else false end
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and p.id = :projectId
        """,
    )
    override fun existsActiveSprintByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): Boolean

    @Query(
        """
        select s.id
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and p.id = :projectId
        """,
    )
    override fun findActiveSprintIdsByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): List<UUID>

    override fun findActiveSprintIdsByProjectIds(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): Map<UUID, Set<UUID>> =
        if (projectIds.isEmpty()) {
            emptyMap()
        } else {
            findProjectSprintRows(projectIds, workspaceId)
                .groupBy(
                    keySelector = { row: ProjectSprintRow -> row.getProjectId() },
                    valueTransform = { row: ProjectSprintRow -> row.getSprintId() },
                ).mapValues { entry -> entry.value.toSet() }
        }

    @Query(
        """
        select case when count(s) > 0 then true else false end
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
            join pizza.psycho.sos.project.project.domain.model.entity.ProjectTaskMapping ptm
                on ptm.project = p
            join pizza.psycho.sos.project.task.domain.model.entity.Task t
                on t.id = ptm.taskId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and t.deletedAt is null
          and s.id = :sprintId
          and ptm.taskId = :taskId
        """,
    )
    override fun existsActiveSprintByTaskIdAndSprintId(
        taskId: UUID,
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): Boolean

    @Query(
        """
        select s
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and p.id = :projectId
        """,
    )
    override fun findActiveSprintsByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): List<Sprint>

    override fun findActiveSprintsByProjectIds(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<Sprint> =
        if (projectIds.isEmpty()) {
            emptyList()
        } else {
            findDistinctActiveSprintsByProjectIds(projectIds, workspaceId)
        }

    @Query(
        """
        select distinct s
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
            join pizza.psycho.sos.project.project.domain.model.entity.ProjectTaskMapping ptm
                on ptm.project = p
            join pizza.psycho.sos.project.task.domain.model.entity.Task t
                on t.id = ptm.taskId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and t.deletedAt is null
          and ptm.taskId = :taskId
        """,
    )
    override fun findActiveSprintsByTaskId(
        taskId: UUID,
        workspaceId: WorkspaceId,
    ): List<Sprint>

    override fun findActiveSprints(
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Sprint> = findAllByWorkspaceIdValueAndDeletedAtIsNull(workspaceId.value, pageable)

    @Query(
        """
        select sp.projectId as projectId, s.id as sprintId
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and p.id in :projectIds
        """,
    )
    fun findProjectSprintRows(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectSprintRow>

    @Query(
        """
        select ptm.taskId as taskId, s.id as sprintId
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
            join pizza.psycho.sos.project.project.domain.model.entity.ProjectTaskMapping ptm
                on ptm.project = p
            join pizza.psycho.sos.project.task.domain.model.entity.Task t
                on t.id = ptm.taskId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and t.deletedAt is null
          and ptm.taskId in :taskIds
        """,
    )
    fun findTaskSprintRows(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskSprintRow>

    @Query(
        """
        select distinct s
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and p.id in :projectIds
        """,
    )
    fun findDistinctActiveSprintsByProjectIds(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<Sprint>

    fun findByIdAndWorkspaceIdValueAndDeletedAtIsNull(
        id: UUID,
        workspaceId: UUID,
    ): Sprint?

    fun findAllByWorkspaceIdValueAndDeletedAtIsNull(
        workspaceId: UUID,
        pageable: Pageable,
    ): Page<Sprint>
}

interface ProjectSprintRow {
    fun getProjectId(): UUID

    fun getSprintId(): UUID
}

interface TaskSprintRow {
    fun getTaskId(): UUID

    fun getSprintId(): UUID
}
