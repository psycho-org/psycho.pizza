package pizza.psycho.sos.common.message.channel.mail.send.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.util.UUID

sealed interface MailSendRequest {
    data class General(
        @field:NotBlank
        val to: String,
        @field:NotBlank
        val subject: String,
        @field:NotBlank
        val htmlContent: String,
        val from: String? = null,
    )

    data class Send(
        @field:NotBlank
        val to: String,
        @field:NotBlank
        val mailType: String,
        val params: Map<String, String?> = emptyMap(),
    )

    data class Otp(
        @field:NotBlank
        val to: String,
        @field:NotBlank
        val otpCode: String,
        val otpPurpose: String? = null,
        @field:Positive
        val expiresInMinutes: Long = 5,
    )

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
