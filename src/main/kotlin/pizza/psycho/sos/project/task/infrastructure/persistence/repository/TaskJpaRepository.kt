package pizza.psycho.sos.project.task.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.task.domain.model.entity.Task
import pizza.psycho.sos.project.task.domain.repository.TaskRepository
import java.util.UUID

@Repository
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

    fun findByIdAndWorkspaceIdValue(
        id: UUID,
        workspaceId: UUID,
    ): Task?

    fun findByIdAndWorkspaceIdValueAndDeletedAtIsNull(
        id: UUID,
        workspaceId: UUID,
    ): Task?
}
