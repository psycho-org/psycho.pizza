package pizza.psycho.sos.workspace.domain.model.workspace

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseDeletableEntity
import pizza.psycho.sos.workspace.domain.model.membership.Membership
import pizza.psycho.sos.workspace.domain.model.membership.Role
import java.util.UUID

@Entity
@Table(name = "workspaces")
class Workspace protected constructor(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "description")
    var description: String,
) : BaseDeletableEntity() {
    @OneToMany(
        mappedBy = "workspace",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    val memberships: MutableSet<Membership> = HashSet()

    companion object {
        fun create(
            name: String,
            description: String,
            ownerAccountId: UUID,
            ownerDisplayName: String = "",
        ): Workspace {
            val workspace = Workspace(name, description)
            val ownerMembership =
                Membership.create(
                    workspace = workspace,
                    accountId = ownerAccountId,
                    displayName = ownerDisplayName,
                    role = Role.OWNER,
                )
            workspace.memberships.add(ownerMembership)
            return workspace
        }
    }

    fun addMembership(
        accountId: UUID,
        displayName: String = "",
        role: Role = Role.CREW,
    ): Membership {
        if (memberships.any { it.accountId == accountId && !it.isDeleted }) {
            throw IllegalArgumentException("membership already exists for accountId=$accountId")
        }
        val membership = Membership.create(workspace = this, accountId = accountId, displayName = displayName, role = role)
        memberships.add(membership)
        return membership
    }

    fun updateMembershipRole(
        accountId: UUID,
        role: Role,
    ): Membership {
        val membership =
            memberships.firstOrNull { it.accountId == accountId && !it.isDeleted }
                ?: throw IllegalArgumentException("membership not found for accountId=$accountId")
        membership.role = role
        return membership
    }

    fun transferOwnership(
        requesterAccountId: UUID,
        newOwnerAccountId: UUID,
    ): Membership {
        val requesterMembership =
            memberships.firstOrNull { it.accountId == requesterAccountId && !it.isDeleted }
                ?: throw IllegalArgumentException("membership not found for requesterAccountId=$requesterAccountId")

        if (!requesterMembership.role.isOwner()) {
            throw IllegalArgumentException("only owner can transfer ownership")
        }

        val newOwnerMembership =
            memberships.firstOrNull { it.accountId == newOwnerAccountId && !it.isDeleted }
                ?: throw IllegalArgumentException("membership not found for newOwnerAccountId=$newOwnerAccountId")

        if (requesterMembership.accountId == newOwnerMembership.accountId) {
            return newOwnerMembership
        }

        requesterMembership.role = Role.CREW
        newOwnerMembership.role = Role.OWNER
        return newOwnerMembership
    }

    fun removeMembership(
        accountId: UUID,
        deletedBy: UUID,
    ): Membership {
        val membership =
            memberships.firstOrNull { it.accountId == accountId && !it.isDeleted }
                ?: throw IllegalArgumentException("membership not found for accountId=$accountId")
        membership.delete(deletedBy)
        return membership
    }

    fun removeAllMemberships(deletedBy: UUID) {
        memberships.forEach { membership ->
            if (!membership.isDeleted) {
                membership.delete(deletedBy)
            }
        }
    }
}
