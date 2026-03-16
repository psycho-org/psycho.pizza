package pizza.psycho.sos.project.reason.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.project.common.domain.model.vo.WorkspaceId
import pizza.psycho.sos.project.reason.application.port.inbound.usecase.RecordReasonUseCase
import pizza.psycho.sos.project.reason.application.port.inbound.usecase.command.RecordReasonCommand
import pizza.psycho.sos.project.reason.domain.model.entity.Reason
import pizza.psycho.sos.project.reason.domain.repository.ReasonRepository

@Service
class ReasonService(
    private val reasonRepository: ReasonRepository,
) : RecordReasonUseCase {
    override fun record(command: RecordReasonCommand) {
        // MVP에서는 중복 기록을 완전히 막지 않고, 동일 eventId에 대한 중복 저장만 선조회로 완화한다.
        if (reasonRepository.existsByEventId(command.eventId)) {
            return
        }
        reasonRepository.save(
            Reason(
                reason = command.reason,
                targetId = command.targetId,
                targetType = command.targetType,
                eventId = command.eventId,
                eventType = command.eventType,
                workspaceId = WorkspaceId(command.workspaceId),
            ),
        )
    }
}
