package pizza.psycho.sos.project.reason.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.reason.application.event.ReasonInternalEvent
import pizza.psycho.sos.project.reason.application.port.inbound.usecase.RecordReasonUseCase
import pizza.psycho.sos.project.reason.domain.model.entity.Reason
import pizza.psycho.sos.project.reason.domain.repository.ReasonRepository

@Service
class ReasonService(
    private val reasonRepository: ReasonRepository,
) : RecordReasonUseCase {
    override fun record(event: ReasonInternalEvent) {
        reasonRepository.save(
            Reason(
                reason = event.reason,
                targetId = event.targetId,
                targetType = event.targetType,
                eventId = event.eventId,
                eventType = event.reasonEventType,
                workspaceId = WorkspaceId(event.workspaceId),
            ),
        )
    }
}
