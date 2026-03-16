package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest
import pizza.psycho.sos.analysis.domain.exception.AnalysisErrorCode
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
) {
    @Transactional
    fun markRunning(id: UUID) {
        val analysisRequest = getAnalysisRequestEntity(id)
        analysisRequest.markAsRunning()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // NOTE: 기존 롤백과 무관하게 무조건 커밋됨
    fun fail(
        id: UUID,
        errorMessage: String,
    ) {
        val analysisRequest = getAnalysisRequestEntity(id)
        analysisRequest.markAsFailed(errorMessage)
    }

    @Transactional
    fun completeAnalysis(
        id: UUID,
        result: Any?,
    ) {
        val analysisRequest = getAnalysisRequestEntity(id)
        analysisRequest.complete(result)
    }

    private fun getAnalysisRequestEntity(id: UUID): AnalysisRequest =
        analysisRequestRepository
            .findById(id)
            .orElseThrow { DomainException(AnalysisErrorCode.ANALYSIS_REQUEST_NOT_FOUND) }
}
