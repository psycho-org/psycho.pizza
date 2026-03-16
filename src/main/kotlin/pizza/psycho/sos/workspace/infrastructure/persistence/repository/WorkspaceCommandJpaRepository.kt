package pizza.psycho.sos.workspace.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import pizza.psycho.sos.workspace.domain.repository.WorkspaceCommandRepository
import java.util.UUID

@Repository
interface WorkspaceCommandJpaRepository :
    WorkspaceCommandRepository,
    JpaRepository<Workspace, UUID>
