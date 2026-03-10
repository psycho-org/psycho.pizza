package pizza.psycho.sos.common.message.channel.mail.template.domain.repository

import pizza.psycho.sos.common.message.channel.mail.template.domain.model.entity.MailTemplate
import pizza.psycho.sos.common.message.domain.MessageType

interface MailTemplateRepository {
    fun findActiveByMailTypeOrNull(mailType: MessageType): MailTemplate?

    fun findAllActive(): List<MailTemplate>

    fun save(mailTemplate: MailTemplate): MailTemplate
}
