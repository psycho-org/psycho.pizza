package pizza.psycho.sos.common.message.channel.mail.send.presentation.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

sealed interface MailSendRequest {
    data class WorkspaceInvite(
        @field:NotBlank
        val to: String,
        @field:NotBlank
        val workspaceName: String,
        val inviterName: String? = null,
        val inviteLink: String? = null,
        val workspaceId: UUID,
    )
}
