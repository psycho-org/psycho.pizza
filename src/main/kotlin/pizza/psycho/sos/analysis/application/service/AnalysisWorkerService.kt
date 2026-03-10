package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AnalysisWorkerService {
    fun processAnalysisJob(jobId: UUID) {
        /*
         * TODO:
         * 1. 분석 대상 데이터 수집 (snapshot / audit log)
         * 2. 프롬프트 구성
         * 3. LLM API 호출
         * 4. 결과 파싱 및 검증
         * 5. 결과 저장 & 상태 업데이트 (Analysis Report)
         * 6. 클라이언트에 결과 전달 방법에 대한 고민이 필요함(SSE / API)
         */
    }
}
