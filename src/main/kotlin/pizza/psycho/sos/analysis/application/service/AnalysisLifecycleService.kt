package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.analysis.application.service.dto.ParsedAnalysisResult
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisRequestRepository
import pizza.psycho.sos.common.handler.DomainException
import java.util.UUID

/*
 * AnalysisLifecycleService
 * - 상태 전이 / persistence
 */
@Service
class AnalysisLifecycleService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val analysisReportService: AnalysisReportService,
) {
    @Transactional
    fun markRunning(id: UUID) {
        val analysisRequest = getAnalysisRequestEntity(id)
        analysisRequest.markAsRunning()
    }

    @Transactional(readOnly = true)
    fun getAnalysisRequest(id: UUID): AnalysisRequest = getAnalysisRequestEntity(id)

    @Transactional
    fun complete(
        id: UUID,
        result: ParsedAnalysisResult,
    ) {
        val analysisRequest = getAnalysisRequestEntity(id)

        analysisReportService.createReport(
            analysisRequest = analysisRequest,
            result = result,
        )

        analysisRequest.markAsDone()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // NOTE: 기존 롤백과 무관하게 무조건 커밋됨
    fun fail(
        id: UUID,
        errorMessage: String,
    ) {
        val analysisRequest = getAnalysisRequestEntity(id)
        analysisRequest.markAsFailed(errorMessage)
    }

    private fun getAnalysisRequestEntity(id: UUID): AnalysisRequest =
        analysisRequestRepository
            .findById(id)
            .orElseThrow { DomainException("AnalysisRequest not found. id=$id") }
}
