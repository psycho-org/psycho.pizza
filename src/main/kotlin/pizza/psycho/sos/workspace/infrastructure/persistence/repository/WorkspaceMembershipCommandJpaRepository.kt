package pizza.psycho.sos.workspace.infrastructure.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pizza.psycho.sos.workspace.domain.model.membership.Membership
import pizza.psycho.sos.workspace.domain.repository.WorkspaceMembershipCommandRepository
import java.util.UUID

@Repository
interface WorkspaceMembershipCommandJpaRepository :
    WorkspaceMembershipCommandRepository,
    JpaRepository<Membership, UUID> {
    @Query(
        """
        select m
        from Membership m
        join m.workspace w
        where m.accountId = :accountId
          and m.role <> pizza.psycho.sos.workspace.domain.model.membership.Role.OWNER
          and m.deletedAt is null
          and w.deletedAt is null
        """,
    )
    override fun findActiveNonOwnerMembershipsByAccountId(
        @Param("accountId") accountId: UUID,
    ): List<Membership>
}
