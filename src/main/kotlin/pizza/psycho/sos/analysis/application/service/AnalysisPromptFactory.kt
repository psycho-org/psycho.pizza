package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Component
import pizza.psycho.sos.analysis.application.service.dto.AnalysisTargetData

/*
 * AnalysisPromptFactory
 * - prompt 생성
 */
@Component
class AnalysisPromptFactory {
    fun create(targetData: AnalysisTargetData): String =
        $$"""
            너는 프로젝트 스프린트 분석 도우미다.
            
            아래 데이터를 기반으로 현재 스프린트 상태를 분석하라.
            반드시 다음을 포함하라:
            1. 전체 요약
            2. 위험 신호
            3. 우선순위 높은 이슈
            4. 추천 액션
            
            [SNAPSHOT]
            ${targetData.snapshot}
            
            [AUDIT LOGS]
            ${targetData.auditLogs.joinToString(separator = "\n")}
        """.trimIndent()
}
