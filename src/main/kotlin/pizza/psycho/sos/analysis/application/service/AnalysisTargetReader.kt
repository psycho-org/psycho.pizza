package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.service.dto.AnalysisTargetData
import pizza.psycho.sos.analysis.domain.entity.AnalysisRequest

/*
 * AnalysisTargetReader
 * - 분석 데이터 조회
 */
@Component
class AnalysisTargetReader {
    fun read(analysisRequest: AnalysisRequest): AnalysisTargetData {
        // TODO: audit log, snapshot 가져와서 dto로 래핑

        return AnalysisTargetData(
            snapshot = snapshot,
            auditLogs = auditLogs,
        )
    }
}
