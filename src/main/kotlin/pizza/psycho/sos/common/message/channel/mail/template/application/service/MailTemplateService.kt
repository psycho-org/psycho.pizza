package pizza.psycho.sos.common.message.channel.mail.template.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.common.handler.DomainException
import pizza.psycho.sos.common.message.channel.mail.template.application.render.MailTemplateRenderer
import pizza.psycho.sos.common.message.channel.mail.template.domain.data.MailTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.entity.MailTemplate
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.vo.RenderedMailTemplate
import pizza.psycho.sos.common.message.channel.mail.template.domain.repository.MailTemplateRepository
import pizza.psycho.sos.common.message.channel.mail.template.domain.spec.MailTemplateSpecRegistry
import pizza.psycho.sos.common.message.domain.MessageType

@Service
class MailTemplateService(
    private val mailTemplateRepository: MailTemplateRepository,
) {
    private val renderer = MailTemplateRenderer()

    @Transactional(readOnly = true)
    fun render(data: MailTemplateData): RenderedMailTemplate {
        logger.info("Rendering mail template. mailType={}", data.mailType)
        val template = findTemplateByType(data.mailType)
        val spec = MailTemplateSpecRegistry.get(data.mailType)

        return try {
            renderer.render(template, data, spec)
        } catch (ex: IllegalArgumentException) {
            logger.warn(
                "Failed to render mail template. mailType={} reason={}",
                data.mailType,
                ex.message,
            )
            throw DomainException(ex.message ?: "failed to render mail template", ex)
        }
    }

    private fun findTemplateByType(mailType: MessageType): MailTemplate =
        mailTemplateRepository.findActiveByMailTypeOrNull(mailType)
            ?: run {
                logger.warn("Mail template not found. mailType={}", mailType)
                throw DomainException("mail template not found. mailType=$mailType")
            }

    @Transactional(readOnly = true)
    fun getActiveTemplate(mailType: MessageType): MailTemplate = findTemplateByType(mailType)

    @Transactional(readOnly = true)
    fun getActiveTemplates(): List<MailTemplate> = mailTemplateRepository.findAllActive()

    companion object {
        private val logger = LoggerFactory.getLogger(MailTemplateService::class.java)
    }
}
