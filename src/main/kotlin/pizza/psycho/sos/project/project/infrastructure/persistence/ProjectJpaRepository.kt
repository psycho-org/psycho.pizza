package pizza.psycho.sos.project.project.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
import pizza.psycho.sos.project.project.domain.event.ProjectDeletedEvent
import pizza.psycho.sos.project.project.domain.model.entity.Project
import java.util.UUID

@Component
interface ProjectJpaRepository :
    ProjectRepository,
    JpaRepository<Project, UUID> {
    override fun findActiveProjectByIdOrNull(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): Project? = findByIdAndWorkspaceIdValueAndDeletedAtIsNull(projectId, workspaceId.value)

    override fun findProgressByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): ProjectProgress? = findProgressByProjectId(projectId, workspaceId.value)

    override fun findProgressesByProjectId(
        projectIds: List<UUID>,
        workspaceId: WorkspaceId,
    ): List<ProjectProgress> = findProgressesByProjectId(projectIds, workspaceId.value)

    override fun findActiveProjectsByIdIn(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<Project> = findAllByIdInAndWorkspaceIdValueAndDeletedAtIsNull(projectIds, workspaceId.value)

    override fun deleteById(
        projectId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): Int =
        findByIdAndWorkspaceIdValueAndDeletedAtIsNull(projectId, workspaceId.value)
            ?.also { it.delete(deletedBy, reason) }
            ?.let { 1 }
            ?: 0

    override fun deleteByIdIn(
        projectIds: Collection<UUID>,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
        reason: String?,
    ): List<ProjectDeletedEvent> {
        if (projectIds.isEmpty()) {
            return emptyList()
        }

        return findAllByIdInAndWorkspaceIdValueAndDeletedAtIsNull(projectIds, workspaceId.value)
            .onEach { it.delete(deletedBy, reason) }
            .flatMap { project -> project.pullDomainEvents().filterIsInstance<ProjectDeletedEvent>() }
    }

    @Query(
        """
    SELECT new pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress(
        p.id,
        count(t.id),
        count(CASE WHEN t.status = 'DONE' THEN 1 END)
    )
    FROM Project p
    LEFT JOIN ProjectTaskMapping ptm ON ptm.project = p
    LEFT JOIN Task t ON t.id = ptm.taskId
    WHERE p.id = :projectId AND t.workspaceId.value = :workspaceId AND p.deletedAt IS NULL AND t.deletedAt IS NULL
    GROUP BY p.id
    """,
    )
    fun findProgressByProjectId(
        projectId: UUID,
        workspaceId: UUID,
    ): ProjectProgress?

    @Query(
        """
    SELECT new pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress(
        p.id,
        count(t.id),
        count(CASE WHEN t.status = 'DONE' THEN 1 END)
    )
    FROM Project p
    LEFT JOIN ProjectTaskMapping ptm ON ptm.project = p
    LEFT JOIN Task t ON t.id = ptm.taskId
    WHERE p.id IN :projectIds 
      AND p.workspaceId.value = :workspaceId 
      AND p.deletedAt IS NULL 
      AND t.deletedAt IS NULL
    GROUP BY p.id
    """,
    )
    fun findProgressesByProjectId(
        projectIds: List<UUID>,
        workspaceId: UUID,
    ): List<ProjectProgress>

    @Query(
        """
        select new pizza.psycho.sos.project.project.application.port.out.dto.TaskAssignment(
            ptm.taskId,
            p.id
        )
        from ProjectTaskMapping ptm
            join ptm.project p
        where ptm.taskId in :taskIds
          and p.workspaceId.value = :workspaceId
          and p.deletedAt is null
        """,
    )
    fun findAssignmentsByTaskIds(
        taskIds: Collection<UUID>,
        workspaceId: UUID,
    ): List<TaskAssignment>

    override fun findActiveProjectIdsByTaskIds(
        taskIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<TaskAssignment> =
        if (taskIds.isEmpty()) {
            emptyList()
        } else {
            findAssignmentsByTaskIds(taskIds, workspaceId.value)
        }

    @Query(
        """
        select ptm.taskId
        from ProjectTaskMapping ptm
            join ptm.project p
            join Task t on t.id = ptm.taskId
        where p.id = :projectId
          and p.workspaceId.value = :workspaceId
          and p.deletedAt is null
          and t.deletedAt is null
        """,
    )
    fun findTaskIdsByProjectId(
        projectId: UUID,
        workspaceId: UUID,
    ): List<UUID>

    override fun findActiveTaskIdsByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
    ): List<UUID> = findTaskIdsByProjectId(projectId, workspaceId.value)

    @Query(
        value =
            """
            select ptm.taskId
            from ProjectTaskMapping ptm
                join ptm.project p
                join Task t on t.id = ptm.taskId
            where p.id = :projectId
              and p.workspaceId.value = :workspaceId
              and p.deletedAt is null
              and t.deletedAt is null
            order by ptm.id
            """,
        countQuery =
            """
            select count(ptm.taskId)
            from ProjectTaskMapping ptm
                join ptm.project p
                join Task t on t.id = ptm.taskId
            where p.id = :projectId
              and p.workspaceId.value = :workspaceId
              and p.deletedAt is null
              and t.deletedAt is null
            """,
    )
    fun findTaskIdsPageByProjectId(
        projectId: UUID,
        workspaceId: UUID,
        pageable: Pageable,
    ): Page<UUID>

    override fun findActiveTaskIdsByProjectId(
        projectId: UUID,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<UUID> = findTaskIdsPageByProjectId(projectId, workspaceId.value, pageable)

    @Query(
        """
        select p.id as projectId, ptm.taskId as taskId
        from ProjectTaskMapping ptm
            join ptm.project p
            join Task t on t.id = ptm.taskId
        where p.id in :projectIds
          and p.workspaceId.value = :workspaceId
          and p.deletedAt is null
          and t.deletedAt is null
        order by ptm.id
        """,
    )
    fun findProjectTaskRows(
        projectIds: Collection<UUID>,
        workspaceId: UUID,
    ): List<ProjectTaskRow>

    override fun findActiveTaskIdsByProjectIds(
        projectIds: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): Map<UUID, List<UUID>> =
        if (projectIds.isEmpty()) {
            emptyMap()
        } else {
            findProjectTaskRows(projectIds, workspaceId.value)
                .groupBy(
                    keySelector = { row: ProjectTaskRow -> row.getProjectId() },
                    valueTransform = { row: ProjectTaskRow -> row.getTaskId() },
                )
        }

    fun findByIdAndWorkspaceIdValueAndDeletedAtIsNull(
        id: UUID,
        workspaceId: UUID,
    ): Project?

    fun findAllByIdInAndWorkspaceIdValueAndDeletedAtIsNull(
        ids: Collection<UUID>,
        workspaceId: UUID,
    ): List<Project>
}

interface ProjectTaskRow {
    fun getProjectId(): UUID

    fun getTaskId(): UUID
}
