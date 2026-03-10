package pizza.psycho.sos.audit.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import pizza.psycho.sos.audit.domain.vo.AuditEventType
import pizza.psycho.sos.audit.domain.vo.AuditTargetType
import pizza.psycho.sos.common.entity.BaseEntity
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_log")
class AuditLog(
    @Column(name = "workspace_id", nullable = false, updatable = false)
    val workspaceId: UUID,
    @Column(name = "actor_id")
    val actorId: UUID?,
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50, updatable = false)
    val targetType: AuditTargetType,
    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    val auditEventType: AuditEventType,
    @Column(name = "from_value", columnDefinition = "TEXT", updatable = false)
    val fromValue: String?,
    @Column(name = "to_value", columnDefinition = "TEXT", updatable = false)
    val toValue: String?,
    @Column(name = "occurred_at", nullable = false, updatable = false)
    val occurredAt: Instant,
) : BaseEntity()
