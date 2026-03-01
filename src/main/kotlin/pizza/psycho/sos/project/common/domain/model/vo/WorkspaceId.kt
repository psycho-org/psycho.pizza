package pizza.psycho.sos.project.common.domain.model.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
data class WorkspaceId(
    @Column(name = "workspace_id", nullable = false)
    val value: UUID,
)
