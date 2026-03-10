package pizza.psycho.sos.common.message.channel.mail.template.presentation.dto

sealed interface MailTemplateResponse {
    data class Summary(
        val mailType: String,
        val title: String,
        val description: String,
        val actionType: String? = null,
        val tokenAuthEnabled: Boolean,
        val tokenExpireHours: Long?,
        val variables: List<Variable>,
    )

    data class Variable(
        val name: String,
        val required: Boolean,
        val description: String? = null,
    )
}
