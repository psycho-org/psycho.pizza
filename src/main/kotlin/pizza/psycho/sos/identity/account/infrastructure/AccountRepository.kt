package pizza.psycho.sos.identity.account.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pizza.psycho.sos.identity.account.domain.Account
import java.util.UUID

interface AccountRepository : JpaRepository<Account, UUID> {
    fun existsByEmailValueIgnoreCaseAndDeletedAtIsNull(email: String): Boolean

    fun findByEmailValueIgnoreCaseAndDeletedAtIsNull(email: String): Account?

    fun findByIdAndDeletedAtIsNull(id: UUID): Account?

    @Query(
        """
        select new pizza.psycho.sos.identity.account.infrastructure.ActiveAccountPrincipalView(a.id, a.email.value)
        from Account a
        where a.id = :accountId
          and a.deletedAt is null
        """,
    )
    fun findActivePrincipalViewById(
        @Param("accountId") accountId: UUID,
    ): ActiveAccountPrincipalView?
}
