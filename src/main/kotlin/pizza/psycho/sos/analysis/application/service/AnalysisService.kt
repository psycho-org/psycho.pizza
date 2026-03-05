package pizza.psycho.sos.analysis.application.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import pizza.psycho.sos.analysis.application.service.dto.AnalysisCommand
import pizza.psycho.sos.analysis.application.service.dto.AnalysisResult
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisRequestRepository

@Service
class AnalysisService(
    private val analysisRequestRepository: AnalysisRequestRepository,
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

        // TODO: Queue에 Push

        return AnalysisResult.Created(
            // TODO: domain exception
            id = requireNotNull(saved.id) { "저장 후 ID가 생성되어야 합니다." },
            status = saved.status.name,
            createdAt = requireNotNull(saved.createdAt) { "저장 후 생성 시간이 기록되어야 합니다." },
        )
    }
}
