package pizza.psycho.sos.workspace.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pizza.psycho.sos.workspace.domain.model.membership.Membership
import pizza.psycho.sos.workspace.domain.model.membership.Role
import pizza.psycho.sos.workspace.domain.repository.MembershipRepository
import java.util.UUID

@Repository
interface MembershipJpaRepository :
    MembershipRepository,
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

}
