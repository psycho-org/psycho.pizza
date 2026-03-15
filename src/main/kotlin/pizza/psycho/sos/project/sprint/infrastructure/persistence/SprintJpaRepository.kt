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
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and ptm.taskId = :taskId
        """,
    )
    override fun existsActiveSprintByTaskId(
        taskId: UUID,
        workspaceId: WorkspaceId,
    ): Boolean

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

    @Query(
        """
        select case when count(s) > 0 then true else false end
        from Sprint s
            join SprintProjectMapping sp on sp.sprint = s
            join pizza.psycho.sos.project.project.domain.model.entity.Project p
                on p.id = sp.projectId
            join pizza.psycho.sos.project.project.domain.model.entity.ProjectTaskMapping ptm
                on ptm.project = p
        where s.workspaceId = :workspaceId
          and s.deletedAt is null
          and p.deletedAt is null
          and s.id = :sprintId
          and ptm.taskId = :taskId
        """,
    )
    override fun existsActiveSprintByTaskIdAndSprintId(
        taskId: UUID,
        sprintId: UUID,
        workspaceId: WorkspaceId,
    ): Boolean

    override fun findActiveSprints(
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Sprint> = findAllByWorkspaceIdValueAndDeletedAtIsNull(workspaceId.value, pageable)

    fun findByIdAndWorkspaceIdValueAndDeletedAtIsNull(
        id: UUID,
        workspaceId: UUID,
    ): Sprint?

    fun findAllByWorkspaceIdValueAndDeletedAtIsNull(
        workspaceId: UUID,
        pageable: Pageable,
    ): Page<Sprint>
}
