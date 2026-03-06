package pizza.psycho.sos.analysis.application.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AnalysisWorkerService {
    fun processAnalysisJob(jobId: UUID) {
        // TODO: LLM API 호출
    }
}
