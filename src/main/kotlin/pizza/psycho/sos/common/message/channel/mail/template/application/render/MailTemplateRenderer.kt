package pizza.psycho.sos.common.message.channel.mail.template.application.render

import pizza.psycho.sos.common.message.channel.mail.template.domain.data.MailTemplateData
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.entity.MailTemplate
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.vo.MailTemplateVariables
import pizza.psycho.sos.common.message.channel.mail.template.domain.model.vo.RenderedMailTemplate
import pizza.psycho.sos.common.message.channel.mail.template.domain.spec.MailTemplateSpec

class MailTemplateRenderer {
    fun render(
        template: MailTemplate,
        data: MailTemplateData,
        spec: MailTemplateSpec,
    ): RenderedMailTemplate {
        require(template.mailType == data.mailType) {
            "Template type mismatch. template=${template.mailType}, data=${data.mailType}"
        }
        require(spec.mailType == data.mailType) {
            "Template spec mismatch. spec=${spec.mailType}, data=${data.mailType}"
        }
        val variables = MailTemplateVariables.from(data.variables())
        variables.validatePlaceholders(
            texts = listOf(template.title, template.htmlContent),
            definitions = spec.variables,
        )
        variables.validateRequired(spec.variables)

        return RenderedMailTemplate(
            title = variables.resolve(template.title),
            htmlContent = variables.resolve(template.htmlContent),
        )
    }
}
