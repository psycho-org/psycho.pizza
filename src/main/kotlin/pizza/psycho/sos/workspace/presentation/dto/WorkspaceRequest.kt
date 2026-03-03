package pizza.psycho.sos.workspace.presentation.dto

import jakarta.validation.constraints.NotBlank
import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.util.UUID

sealed interface WorkspaceRequest {
    data class Create(
        @field:NotBlank
        val name: String,
        val description: String,
        val ownerAccountId: UUID,
    )

    data class TransferOwner(
        val requesterAccountId: UUID,
        val newOwnerAccountId: UUID,
    )

    data class Delete(
        val requesterAccountId: UUID,
    )

    data class AddMember(
        val requesterAccountId: UUID,
        val accountId: UUID,
        val role: Role = Role.CREW,
    )

    data class RemoveMember(
        val requesterAccountId: UUID,
        val accountId: UUID,
    )
}
