package pizza.psycho.sos.common.message.action.application.model

import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.message.domain.exception.MessageErrorCode
import java.util.UUID

data class WorkspaceInviteActionParams(
    val workspaceId: UUID,
    val inviterAccountId: UUID,
    val inviteeEmail: String,
) {
    fun toTokenParams(): Map<String, String> =
        mapOf(
            "workspaceId" to workspaceId.toString(),
            "inviterAccountId" to inviterAccountId.toString(),
            "inviteeEmail" to inviteeEmail,
        )

    companion object {
        fun from(values: Map<String, String?>): WorkspaceInviteActionParams =
            WorkspaceInviteActionParams(
                workspaceId = values.requiredUuid("workspaceId"),
                inviterAccountId = values.requiredUuid("inviterAccountId"),
                inviteeEmail = values.requiredEmail("inviteeEmail"),
            )

        private fun Map<String, String?>.required(key: String): String =
            this[key]?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw DomainException(
                    MessageErrorCode.MESSAGE_REQUIRED_FIELD_MISSING,
                    "param '$key' is required",
                )

        private fun Map<String, String?>.requiredUuid(key: String): UUID =
            try {
                UUID.fromString(required(key))
            } catch (ex: IllegalArgumentException) {
                throw DomainException(
                    MessageErrorCode.MESSAGE_INVALID_UUID_PARAM,
                    "param '$key' must be a valid UUID",
                    ex,
                )
            }

        private fun Map<String, String?>.requiredEmail(key: String): String {
            val email = required(key).lowercase()
            if (!email.contains("@")) {
                throw DomainException(
                    MessageErrorCode.MESSAGE_INVALID_EMAIL_PARAM,
                    "param '$key' must be a valid email",
                )
            }
            return email
        }
    }
}
