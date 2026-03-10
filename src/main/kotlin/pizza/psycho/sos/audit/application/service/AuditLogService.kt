package pizza.psycho.sos.audit.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.audit.domain.entity.AuditLog
import pizza.psycho.sos.audit.domain.vo.AuditEventType
import pizza.psycho.sos.audit.domain.vo.AuditTargetType
import pizza.psycho.sos.audit.infrastructure.persistence.AuditLogRepository
import java.time.Instant
import java.util.UUID

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository,
) {
    fun getAuditLogsForAnalysis(targetId: UUID) = auditLogRepository.findByTargetId(targetId)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createAuditLog(
        workspaceId: UUID,
        actorId: UUID?,
        targetType: AuditTargetType,
        targetId: UUID,
        auditEventType: AuditEventType,
        fromValue: String?,
        toValue: String?,
        eventId: UUID,
        occurredAt: Instant,
    ) {
        val auditLog =
            AuditLog(
                workspaceId = workspaceId,
                actorId = actorId,
                targetType = targetType,
                targetId = targetId,
                auditEventType = auditEventType,
                fromValue = fromValue,
                toValue = toValue,
                eventId = eventId,
                occurredAt = occurredAt,
            )
        auditLogRepository.save(auditLog)
    }
}
