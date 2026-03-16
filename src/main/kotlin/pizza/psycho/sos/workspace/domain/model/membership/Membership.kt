package pizza.psycho.sos.workspace.domain.model.membership

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseDeletableEntity
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import java.util.UUID

@Entity
@Table(name = "memberships")
class Membership protected constructor(
    @Column(name = "account_id", nullable = false, updatable = false)
    val accountId: UUID,
    @Column(name = "display_name", nullable = false)
    var displayName: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: Role,
) : BaseDeletableEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false, updatable = false)
    lateinit var workspace: Workspace

    companion object {
        fun create(
            workspace: Workspace,
            accountId: UUID,
            displayName: String = "",
            role: Role = Role.CREW,
        ): Membership {
            val membership = Membership(accountId, displayName, role)
            membership.workspace = workspace
            return membership
        }
    }
}

enum class Role {
    OWNER,
    CREW,
    ;

    fun isOwner() = this == OWNER

    fun isCrew() = this == CREW
}
