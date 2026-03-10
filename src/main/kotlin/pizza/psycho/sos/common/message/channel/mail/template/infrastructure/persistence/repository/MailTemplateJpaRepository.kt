package pizza.psycho.sos.common.message.channel.mail.template.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.entity.MailTemplate
import pizza.psycho.sos.common.message.channel.mail.template.domain.repository.MailTemplateRepository
import pizza.psycho.sos.common.message.domain.MessageType
import java.util.UUID

@Repository
interface MailTemplateJpaRepository :
    MailTemplateRepository,
    JpaRepository<MailTemplate, UUID> {
    override fun findActiveByMailTypeOrNull(mailType: MessageType): MailTemplate? = findByMailTypeAndDeletedAtIsNull(mailType)

    override fun findAllActive(): List<MailTemplate> = findAllByDeletedAtIsNull()

    fun findByMailTypeAndDeletedAtIsNull(mailType: MessageType): MailTemplate?

    fun findAllByDeletedAtIsNull(): List<MailTemplate>
}
