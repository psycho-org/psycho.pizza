package pizza.psycho.sos.workspace.presentation.dto

import jakarta.validation.constraints.NotBlank
import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.util.UUID

sealed interface WorkspaceRequest {
    data class Create(
        @field:NotBlank
        val name: String,
        val description: String,
    )

    data class TransferOwner(
        val newOwnerAccountId: UUID,
    )

    data class AddMember(
        val accountId: UUID,
        val role: Role = Role.CREW,
    )

    data class RemoveMember(
        val accountId: UUID,
    )
}
