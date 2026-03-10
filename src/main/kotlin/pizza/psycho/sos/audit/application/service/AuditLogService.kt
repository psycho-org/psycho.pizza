package pizza.psycho.sos.audit.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.audit.infrastructure.persistence.AuditLogRepository
import java.util.UUID

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository,
) {
    fun getAuditLogsForAnalysis(targetId: UUID) = auditLogRepository.findByTargetId(targetId)
}
