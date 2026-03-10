package pizza.psycho.sos.common.message.channel.mail.template.domain.data

import pizza.psycho.sos.common.message.domain.MessageType

data class OtpTemplateData(
    val otpCode: String,
    val expiresInMinutes: Long,
    val otpPurpose: String? = null,
    val title: String = DEFAULT_TITLE,
) : MailTemplateData {
    override val mailType: MessageType = MessageType.OTP

    override fun variables(): Map<String, String?> =
        mapOf(
            "title" to title,
            "OTP" to otpCode,
            "otpCode" to otpCode,
            "expiresInMinutes" to expiresInMinutes.toString(),
            "otpPurpose" to otpPurpose,
        )

    companion object {
        const val DEFAULT_TITLE: String = "인증번호 안내"
    }
}
