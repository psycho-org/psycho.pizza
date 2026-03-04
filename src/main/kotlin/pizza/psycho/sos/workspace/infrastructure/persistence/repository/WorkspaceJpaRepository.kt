package pizza.psycho.sos.workspace.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import pizza.psycho.sos.workspace.domain.repository.WorkspaceRepository
import java.util.UUID

@Repository
interface WorkspaceJpaRepository :
    WorkspaceRepository,
    JpaRepository<Workspace, UUID> {
    override fun findByIdOrNull(id: UUID): Workspace? = findByIdOrNull(id)

    override fun findActiveByIdOrNull(id: UUID): Workspace? = findByIdAndDeletedAtIsNull(id)

    fun findByIdAndDeletedAtIsNull(id: UUID): Workspace?
}
