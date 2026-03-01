package pizza.psycho.sos.identity.account.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseDeletableEntity

@Entity
@Table(name = "accounts")
class Account protected constructor() : BaseDeletableEntity() {
    @Column(name = "email", nullable = false)
    var email: String? = null
        protected set

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String? = null
        protected set

    @Column(name = "given_name", nullable = false)
    var givenName: String? = null
        protected set

    @Column(name = "family_name", nullable = false)
    var familyName: String? = null
        protected set

    @Column(name = "display_name", nullable = false)
    var displayName: String? = null
        protected set

    companion object {
        fun create(
            email: String,
            passwordHash: String,
            givenName: String,
            familyName: String,
        ): Account =
            Account().apply {
                this.email = email
                this.passwordHash = passwordHash
                this.givenName = givenName
                this.familyName = familyName
                this.displayName = "$givenName $familyName"
            }
    }
}
