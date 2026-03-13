package pizza.psycho.sos.analysis.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import pizza.psycho.sos.analysis.application.service.dto.ParsedAnalysisResult
import pizza.psycho.sos.analysis.domain.entity.AnalysisReport
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest
import pizza.psycho.sos.analysis.domain.exception.AnalysisErrorCode
import pizza.psycho.sos.analysis.infrastructure.persistence.AnalysisReportRepository
import pizza.psycho.sos.common.handler.DomainException

@Service
class AnalysisReportService(
    private val analysisReportRepository: AnalysisReportRepository,
    private val objectMapper: ObjectMapper,
) {
    fun createReport(
        analysisRequest: AnalysisRequest,
        result: ParsedAnalysisResult,
    ) {
        // 1. AnalysisReport 엔티티 생성
        val report =
            AnalysisReport(
                analysisRequestId = analysisRequest.id ?: throw DomainException(AnalysisErrorCode.ANALYSIS_REQUEST_NOT_FOUND),
                workspaceId = analysisRequest.workspaceId,
                targetType = analysisRequest.targetType,
                targetId = analysisRequest.targetId,
                scoreTotal = 100, // TODO: 실제 점수 산출 로직 연동 필요
                scoreVersion = "v1.0", // TODO: 점수 체계 버전 연동 필요
                categoryPenalties = "[]", // TODO: 페널티 계산 결과 연동 필요
                penaltyDetails = "[]", // TODO: 페널티 상세 내역 연동 필요
            )

        // 2. 파싱된 LLM 분석 결과를 JSON 문자열로 변환하여 aiInsight에 첨부
        val aiInsightJson = objectMapper.writeValueAsString(result)
        report.attachAiInsight(aiInsightJson)

        // 3. DB에 저장
        analysisReportRepository.save(report)
    }
}
