package pizza.psycho.sos.identity.account.domain

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseDeletableEntity
import pizza.psycho.sos.identity.account.domain.vo.Email

@Entity
@Table(name = "accounts")
class Account protected constructor() : BaseDeletableEntity() {
    @Embedded
    var email: Email = Email()
        protected set

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = ""
        protected set

    @Column(name = "given_name", nullable = false)
    var givenName: String = ""
        protected set

    @Column(name = "family_name", nullable = false)
    var familyName: String = ""
        protected set

    @Column(name = "display_name", nullable = false)
    var displayName: String = ""
        protected set

    companion object {
        fun create(
            email: Email,
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

    fun updateDisplayName(displayName: String) {
        this.displayName = displayName
    }

    fun updateName(
        givenName: String,
        familyName: String,
    ) {
        this.givenName = givenName
        this.familyName = familyName
    }

    fun updatePasswordHash(passwordHash: String) {
        this.passwordHash = passwordHash
    }
}
