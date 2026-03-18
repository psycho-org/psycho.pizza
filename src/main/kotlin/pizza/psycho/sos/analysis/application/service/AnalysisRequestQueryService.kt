package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pizza.psycho.sos.analysis.domain.exception.AnalysisErrorCode
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisReportRepository
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisRequestRepository
import pizza.psycho.sos.analysis.presentation.dto.AnalysisResponse
import pizza.psycho.sos.common.handler.DomainException
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AnalysisRequestQueryService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val analysisReportRepository: AnalysisReportRepository,
) {
    fun getAnalysisRequests(
        workspaceId: UUID,
        sprintId: UUID,
    ): AnalysisResponse.GetAnalysisRequestList.Response {
        val requests =
            analysisRequestRepository
                .findAllByWorkspaceIdAndTargetIdOrderByCreatedAtDesc(
                    workspaceId = workspaceId,
                    sprintId = sprintId,
                )

        val items =
            requests.map { request ->
                val report = analysisReportRepository.findByAnalysisRequestId(request.id!!)

                AnalysisResponse.GetAnalysisRequestList.Item(
                    analysisRequestId =
                        request.id
                            ?: throw DomainException(AnalysisErrorCode.ANALYSIS_REQUEST_NOT_FOUND),
                    status = request.status,
                    hasReport = report != null,
                    requestedAt =
                        request.createdAt
                            ?: throw DomainException(AnalysisErrorCode.ANALYSIS_REQUEST_NOT_FOUND),
                )
            }

        return AnalysisResponse.GetAnalysisRequestList.Response(
            items = items,
        )
    }

    /*
    fun getAnalysisRequestReport(
        analysisRequestId: UUID,
    ): AnalysisResponse.GetAnalysisRequestReport.Response {
        val request =
            analysisRequestRepository.findById(analysisRequestId)
                .orElseThrow {
                    NoSuchElementException("Analysis request not found. analysisRequestId=$analysisRequestId")
                }

        val report = analysisReportRepository.findByAnalysisRequestId(analysisRequestId)

        return AnalysisResponse.GetAnalysisRequestReport.Response(
            analysisRequestId = request.id!!,
            status = request.status,
            requestedAt = request.createdAt,
            result = report?.let {
                AnalysisResponse.GetAnalysisRequestReport.Result(
                    reportId = it.id!!,
                    summary = it.summary,
                    strength = it.strength,
                    risk = it.risk,
                    recommendation = it.recommendation,
                    reportedAt = it.createdAt,
                )
            },
        )
    }
     */
}
