package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisRequestRepository
import pizza.psycho.sos.common.handler.DomainException
import java.util.UUID

@Service
class AnalysisExecutionService(
    private val analysisRequestRepository: AnalysisRequestRepository,
//    private val analysisReportService: AnalysisReportService,
) {
    @Transactional
    fun markRunning(id: UUID) {
        val analysisRequest = getAnalysisRequestEntity(id)

        analysisRequest.markAsRunning()
    }

    @Transactional(readOnly = true)
    fun getAnalysisRequest(id: UUID): AnalysisRequest = getAnalysisRequestEntity(id)

/*
    @Transactional
    fun complete(jobId: UUID, result: ParsedAnalysisResult) {
        val request = getRequestEntity(jobId)

        analysisReportService.createReport(
            request = request,
            result = result,
        )

        request.markAsDone()
    }
*/
    @Transactional
    fun fail(
        jobId: UUID,
        errorMessage: String,
    ) {
        val analysisRequest = getAnalysisRequestEntity(jobId)
        analysisRequest.markAsFailed(errorMessage)
    }

    private fun getAnalysisRequestEntity(id: UUID): AnalysisRequest =
        analysisRequestRepository
            .findById(id)
            .orElseThrow { DomainException("AnalysisRequest not found. id=$id") }
}
