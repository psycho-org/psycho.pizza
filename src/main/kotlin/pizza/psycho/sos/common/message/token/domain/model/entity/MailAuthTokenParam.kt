package pizza.psycho.sos.common.message.token.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseEntity

@Entity
@Table(name = "message_auth_token_params")
class MailAuthTokenParam protected constructor(
    @Column(name = "name", nullable = false, length = 100)
    var name: String,
    @Column(name = "value", nullable = false, length = 2048)
    var value: String,
) : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_auth_token_id", nullable = false, updatable = false)
    lateinit var mailAuthToken: MailAuthToken

    companion object {
        fun create(
            token: MailAuthToken,
            name: String,
            value: String,
        ): MailAuthTokenParam {
            val param = MailAuthTokenParam(name = name, value = value)
            param.mailAuthToken = token
            return param
        }
    }
}
