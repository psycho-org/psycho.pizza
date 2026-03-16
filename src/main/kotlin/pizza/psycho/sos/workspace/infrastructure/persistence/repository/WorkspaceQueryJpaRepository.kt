package pizza.psycho.sos.workspace.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import pizza.psycho.sos.workspace.domain.repository.WorkspaceQueryRepository
import java.util.UUID

@Repository
interface WorkspaceQueryJpaRepository :
    WorkspaceQueryRepository,
    JpaRepository<Workspace, UUID> {
    override fun findByIdOrNull(id: UUID): Workspace? = findById(id).orElse(null)

    override fun findActiveByIdOrNull(id: UUID): Workspace? = findByIdAndDeletedAtIsNull(id)

    fun findByIdAndDeletedAtIsNull(id: UUID): Workspace?
}
