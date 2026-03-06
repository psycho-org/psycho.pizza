package pizza.psycho.sos.analysis.application.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import pizza.psycho.sos.analysis.application.service.dto.AnalysisCommand
import pizza.psycho.sos.analysis.application.service.dto.AnalysisResult
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest
import pizza.psycho.sos.analysis.domain.event.AnalysisRequestCreatedEvent
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisRequestRepository
import pizza.psycho.sos.common.event.DomainEventPublisher
import pizza.psycho.sos.common.handler.DomainException

@Service
class AnalysisService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val domainEventPublisher: DomainEventPublisher,
) {
    @Transactional
    fun createSprintAnalysisRequest(command: AnalysisCommand.Create): AnalysisResult.Created {
        // TODO: sprint 유효성 검사

        val saved =
            analysisRequestRepository.save(
                AnalysisRequest.create(
                    command.workspaceId,
                    command.sprintId,
                    command.requesterId,
                ),
            )

        val id = saved.id ?: throw DomainException("저장 후 ID가 생성되어야 합니다.")
        val createdAt = saved.createdAt ?: throw DomainException("저장 후 생성 시간이 기록되어야 합니다.")

        // NOTE: commit 단계에서 실패 시 DB와 큐가 불일치할 수 있으므로 이벤트로 처리합니다.
        domainEventPublisher.publish(AnalysisRequestCreatedEvent(id))

        return AnalysisResult.Created(
            id = id,
            status = saved.status.name,
            createdAt = createdAt,
        )
    }
}
