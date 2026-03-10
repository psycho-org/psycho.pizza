package pizza.psycho.sos.project.sprint.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
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

    fun findByIdAndWorkspaceIdValueAndDeletedAtIsNull(
        id: UUID,
        workspaceId: UUID,
    ): Sprint?
}
