package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import pizza.psycho.sos.analysis.application.service.dto.ParsedAnalysisResult
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest

@Service
class AnalysisReportService {
    fun createReport(
        request: AnalysisRequest,
        result: ParsedAnalysisResult,
    ) {
        // TODO: AnalysisReport 엔티티 생성 및 저장
    }
}
