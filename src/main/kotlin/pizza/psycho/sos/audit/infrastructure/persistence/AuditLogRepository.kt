package pizza.psycho.sos.audit.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pizza.psycho.sos.audit.domain.entity.AuditLog
import java.util.UUID

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {
    fun findByTargetId(targetId: UUID): List<AuditLog>
}
