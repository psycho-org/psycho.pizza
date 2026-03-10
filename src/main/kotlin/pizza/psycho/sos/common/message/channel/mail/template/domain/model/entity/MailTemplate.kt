package pizza.psycho.sos.common.message.channel.mail.template.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseDeletableEntity
import pizza.psycho.sos.common.message.action.domain.MailActionType
import pizza.psycho.sos.common.message.domain.MessageType

@Entity
@Table(name = "mail_templates")
class MailTemplate(
    @Enumerated(EnumType.STRING)
    @Column(name = "mail_type", nullable = false, length = 50)
    var mailType: MessageType,
    @Column(columnDefinition = "TEXT")
    var title: String,
    @Column
    var description: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50)
    var actionType: MailActionType? = null,
    @Column(name = "token_auth_enabled", nullable = false)
    var tokenAuthEnabled: Boolean = false,
    @Column(name = "token_expire_hours")
    var tokenExpireHours: Long? = null,
    @Column(columnDefinition = "TEXT")
    var htmlContent: String,
) : BaseDeletableEntity()
