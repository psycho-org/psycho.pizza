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
                    hasReport = report?.aiInsight != null,
                    requestedAt =
                        request.createdAt
                            ?: throw DomainException(AnalysisErrorCode.ANALYSIS_REQUEST_NOT_FOUND),
                )
            }

        return AnalysisResponse.GetAnalysisRequestList.Response(
            items = items,
        )
    }

    fun getAnalysisRequestReport(analysisRequestId: UUID): AnalysisResponse.GetAnalysisRequestReport.Response {
        val request =
            analysisRequestRepository
                .findById(analysisRequestId)
                .orElseThrow {
                    DomainException(AnalysisErrorCode.ANALYSIS_REQUEST_NOT_FOUND)
                }

        val report =
            analysisReportRepository.findByAnalysisRequestId(analysisRequestId)

        return AnalysisResponse.GetAnalysisRequestReport.Response(
            workspaceId = request.workspaceId,
            sprintId = request.targetId,
            analysisRequestId = analysisRequestId,
            status = request.status,
            totalScore = report?.scoreTotal ?: 0,
            result = report?.aiInsight, // <- 🔥 TODO: json 형식으로 가도록 2차 수정!
            createdAt = report?.createdAt,
        )
    }
}
