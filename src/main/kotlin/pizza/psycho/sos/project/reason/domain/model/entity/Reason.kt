package pizza.psycho.sos.project.reason.domain.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import pizza.psycho.sos.common.entity.BaseEntity
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.reason.domain.model.vo.EventType
import pizza.psycho.sos.project.reason.domain.model.vo.TargetType
import java.util.UUID

@Table(name = "reasons")
@Entity
class Reason(
    @Column(name = "reason", nullable = false)
    val reason: String,
    @Column(name = "target_id", nullable = false)
    val targetId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    val targetType: TargetType,
    @Column(name = "event_id", nullable = false)
    val eventId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: EventType,
    @Embedded
    val workspaceId: WorkspaceId,
) : BaseEntity()
