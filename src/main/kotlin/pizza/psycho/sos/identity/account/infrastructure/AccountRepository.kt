package pizza.psycho.sos.identity.account.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import pizza.psycho.sos.identity.account.domain.Account
import java.util.UUID

interface AccountRepository : JpaRepository<Account, UUID> {
    fun existsByEmailIgnoreCaseAndDeletedAtIsNull(email: String): Boolean

    fun findByEmailIgnoreCaseAndDeletedAtIsNull(email: String): Account?

    fun findByIdAndDeletedAtIsNull(id: UUID): Account?
}
