package pizza.psycho.sos.common.message.channel.mail.template.domain.spec

import pizza.psycho.sos.common.message.domain.MessageType

data class MailTemplateVariableSpec(
    val name: String,
    val required: Boolean,
    val description: String? = null,
)

data class MailTemplateSpec(
    val mailType: MessageType,
    val variables: List<MailTemplateVariableSpec>,
)

object MailTemplateSpecRegistry {
    private val specs: Map<MessageType, MailTemplateSpec> =
        mapOf(
            MessageType.OTP to
                MailTemplateSpec(
                    mailType = MessageType.OTP,
                    variables =
                        listOf(
                            MailTemplateVariableSpec(
                                name = "otpCode",
                                required = true,
                                description = "인증번호",
                            ),
                            MailTemplateVariableSpec(
                                name = "expiresInMinutes",
                                required = true,
                                description = "인증번호 만료 시간(분)",
                            ),
                            MailTemplateVariableSpec(
                                name = "otpPurpose",
                                required = false,
                                description = "OTP 사용 목적",
                            ),
                        ),
                ),
            MessageType.WORKSPACE_INVITE to
                MailTemplateSpec(
                    mailType = MessageType.WORKSPACE_INVITE,
                    variables =
                        listOf(
                            MailTemplateVariableSpec(
                                name = "workspaceName",
                                required = true,
                                description = "워크스페이스 이름",
                            ),
                            MailTemplateVariableSpec(
                                name = "inviteLink",
                                required = true,
                                description = "인증 링크",
                            ),
                            MailTemplateVariableSpec(
                                name = "inviterName",
                                required = false,
                                description = "초대한 사람",
                            ),
                        ),
                ),
        )

    fun get(mailType: MessageType): MailTemplateSpec =
        specs[mailType] ?: throw IllegalArgumentException("mail template spec not found. mailType=$mailType")
}
