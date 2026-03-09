package pizza.psycho.sos.project.project.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.project.application.port.out.ProjectRepository
import pizza.psycho.sos.project.project.application.port.out.query.ProjectProgress
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

    override fun deleteById(
        projectId: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int =
        findByIdAndWorkspaceIdValueAndDeletedAtIsNull(projectId, workspaceId.value)
            ?.also { it.delete(deletedBy) }
            ?.let { 1 }
            ?: 0

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

    fun findByIdAndWorkspaceIdValueAndDeletedAtIsNull(
        id: UUID,
        workspaceId: UUID,
    ): Project?
}
