package pizza.psycho.sos.project.task.infrastructure.persistence.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.domain.repository.TaskRepository
import java.util.UUID

@Repository
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

    override fun deleteById(
        id: UUID,
        deletedBy: UUID,
        workspaceId: WorkspaceId,
    ): Int =
        findByIdAndWorkspaceIdValueAndDeletedAtIsNull(id, workspaceId.value)
            ?.also { it.delete(deletedBy) }
            ?.let { 1 }
            ?: 0

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
}
