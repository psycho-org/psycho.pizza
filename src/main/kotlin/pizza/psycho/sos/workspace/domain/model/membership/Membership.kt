package pizza.psycho.sos.workspace.domain.model.membership

import jakarta.persistence.*
import pizza.psycho.sos.common.entity.BaseDeletableEntity
import pizza.psycho.sos.workspace.domain.model.workspace.Workspace
import java.util.UUID

@Entity
@Table(name = "memberships")
class Membership protected constructor(
    @Column(name = "account_id", nullable = false, updatable = false)
    val accountId: UUID,
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
            role: Role = Role.CREW,
        ): Membership {
            val membership = Membership(accountId, role)
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
