package pizza.psycho.sos.project.task.infrastructure.persistence.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.domain.repository.TaskRepository
import java.util.UUID

@Component
interface TaskJpaRepository :
    TaskRepository,
    JpaRepository<Task, UUID> {
    override fun findActiveTaskByIdOrNull(
        id: UUID,
        workspaceId: WorkspaceId,
    ): Task? = findByIdAndWorkspaceIdValueAndDeletedAtIsNull(id, workspaceId.value)

    override fun findByIdOrNull(
        id: UUID,
        workspaceId: WorkspaceId,
    ): Task? = findByIdAndWorkspaceIdValue(id, workspaceId.value)

    override fun findAll(
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Task> = findAllByWorkspaceIdValue(workspaceId.value, pageable)

    override fun findAllActiveTasks(
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Task> = findAllByWorkspaceIdValueAndDeletedAtIsNull(workspaceId.value, pageable)

    @Query(
        value =
            """
            select t
            from Task t
            where t.workspaceId.value = :#{#workspaceId.value}
              and t.deletedAt is null
              and not exists (
                  select s.id
                  from pizza.psycho.sos.project.sprint.domain.model.entity.Sprint s
                      join pizza.psycho.sos.project.sprint.domain.model.entity.SprintProjectMapping sp on sp.sprint = s
                      join pizza.psycho.sos.project.project.domain.model.entity.Project p on p.id = sp.projectId
                      join pizza.psycho.sos.project.project.domain.model.entity.ProjectTaskMapping ptm on ptm.project = p
                  where s.workspaceId.value = :#{#workspaceId.value}
                    and s.deletedAt is null
                    and p.workspaceId.value = :#{#workspaceId.value}
                    and p.deletedAt is null
                    and ptm.taskId = t.id
              )
            """,
        countQuery =
            """
            select count(t)
            from Task t
            where t.workspaceId.value = :#{#workspaceId.value}
              and t.deletedAt is null
              and not exists (
                  select s.id
                  from pizza.psycho.sos.project.sprint.domain.model.entity.Sprint s
                      join pizza.psycho.sos.project.sprint.domain.model.entity.SprintProjectMapping sp on sp.sprint = s
                      join pizza.psycho.sos.project.project.domain.model.entity.Project p on p.id = sp.projectId
                      join pizza.psycho.sos.project.project.domain.model.entity.ProjectTaskMapping ptm on ptm.project = p
                  where s.workspaceId.value = :#{#workspaceId.value}
                    and s.deletedAt is null
                    and p.workspaceId.value = :#{#workspaceId.value}
                    and p.deletedAt is null
                    and ptm.taskId = t.id
              )
            """,
    )
    override fun findAllActiveBacklogTasks(
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Task>

    override fun findAllByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
    ): List<Task> = findAllByIdInAndWorkspaceIdValueAndDeletedAtIsNull(ids, workspaceId.value)

    override fun findAllByIdIn(
        ids: Collection<UUID>,
        workspaceId: WorkspaceId,
        pageable: Pageable,
    ): Page<Task> = findAllByIdInAndWorkspaceIdValueAndDeletedAtIsNull(ids, workspaceId.value, pageable)

    fun findByIdAndWorkspaceIdValue(
        id: UUID,
        workspaceId: UUID,
    ): Task?

    fun findByIdAndWorkspaceIdValueAndDeletedAtIsNull(
        id: UUID,
        workspaceId: UUID,
    ): Task?

    fun findAllByWorkspaceIdValue(
        workspaceId: UUID,
        pageable: Pageable,
    ): Page<Task>

    fun findAllByWorkspaceIdValueAndDeletedAtIsNull(
        workspaceId: UUID,
        pageable: Pageable,
    ): Page<Task>

    fun findAllByIdInAndWorkspaceIdValueAndDeletedAtIsNull(
        ids: Collection<UUID>,
        workspaceId: UUID,
    ): List<Task>

    fun findAllByIdInAndWorkspaceIdValueAndDeletedAtIsNull(
        ids: Collection<UUID>,
        workspaceId: UUID,
        pageable: Pageable,
    ): Page<Task>
}
