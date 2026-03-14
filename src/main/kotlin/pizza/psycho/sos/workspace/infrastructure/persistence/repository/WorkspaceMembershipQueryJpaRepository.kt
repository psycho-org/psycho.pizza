package pizza.psycho.sos.workspace.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pizza.psycho.sos.workspace.application.dto.ActiveWorkspaceMembership
import pizza.psycho.sos.workspace.application.dto.WorkspaceMemberListItem
import pizza.psycho.sos.workspace.domain.model.membership.Membership
import pizza.psycho.sos.workspace.domain.model.membership.Role
import pizza.psycho.sos.workspace.domain.repository.WorkspaceMembershipQueryRepository
import java.util.UUID

@Repository
interface WorkspaceMembershipQueryJpaRepository :
    WorkspaceMembershipQueryRepository,
    JpaRepository<Membership, UUID> {
    @Query(
        """
        select m.role
        from Membership m
        where m.workspace.id = :workspaceId
          and m.accountId = :accountId
          and m.deletedAt is null
        """,
    )
    override fun findRoleByWorkspaceIdAndAccountId(
        @Param("workspaceId") workspaceId: UUID,
        @Param("accountId") accountId: UUID,
    ): Role?

    @Query(
        """
        select new pizza.psycho.sos.workspace.application.dto.ActiveWorkspaceMembership(
            w.id,
            w.name,
            m.role
        )
        from Membership m
        join m.workspace w
        where m.accountId = :accountId
          and m.deletedAt is null
          and w.deletedAt is null
        """,
    )
    override fun findActiveWorkspaceMembershipsByAccountId(
        @Param("accountId") accountId: UUID,
    ): List<ActiveWorkspaceMembership>

    @Query(
        """
        select new pizza.psycho.sos.workspace.application.dto.WorkspaceMemberListItem(
            m.id,
            m.accountId,
            coalesce(m.displayName, ''),
            m.role,
            m.createdAt
        )
        from Membership m
        where m.workspace.id = :workspaceId
          and m.deletedAt is null
        order by m.createdAt asc
        """,
    )
    override fun findActiveMembersByWorkspaceId(
        @Param("workspaceId") workspaceId: UUID,
    ): List<WorkspaceMemberListItem>
}
