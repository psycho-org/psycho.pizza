package pizza.psycho.sos.common.message.token.application.event

import pizza.psycho.sos.common.message.action.application.model.MailActionRequest
import pizza.psycho.sos.common.message.domain.MessageType

data class MailAuthVerifiedEvent(
    val mailType: MessageType,
    val token: String,
    val actionRequest: MailActionRequest,
)
